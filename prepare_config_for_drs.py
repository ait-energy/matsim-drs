"""
Prepare MATSim config files for use with the drs (dynamic ride sharing) module.
Adjust the integrated XML snippets to your liking before running the script.
"""

import argparse
import logging

from lxml import etree
from pathlib import Path

DRS_MODULE = """
<module name="drs">
    <!-- main drs config, leave empty to use defaults -->
    <param name="maxMatchingDistanceMeters" value="500" />
</module>"""

SCORING_MODEPARAMS_DRIVER = """
<parameterset type="modeParams">
    <param name="constant" value="0.0"/>
    <param name="dailyMonetaryConstant" value="0.0"/>
    <param name="marginalUtilityOfDistance_util_m" value="0.0"/>
    <param name="marginalUtilityOfTraveling_util_hr" value="-6.0"/>
    <param name="mode" value="drsDriver"/>
    <param name="monetaryDistanceRate" value="0.0"/>
</parameterset>
"""

SCORING_MODEPARAMS_RIDER = """
<parameterset type="modeParams">
    <param name="constant" value="0.0"/>
    <param name="dailyMonetaryConstant" value="0.0"/>
    <param name="marginalUtilityOfDistance_util_m" value="0.0"/>
    <param name="marginalUtilityOfTraveling_util_hr" value="-6.0"/>
    <param name="mode" value="drsRider"/>
    <param name="monetaryDistanceRate" value="0.0"/>
</parameterset>
"""

SCORING_ACTIVITYPARAMS_DRIVER = """
<parameterset type="activityParams">
    <param name="activityType" value="drsDriver interaction"/>
    <param name="scoringThisActivityAtAll" value="false"/>
    <param name="typicalDuration" value="undefined"/>
</parameterset>
"""

ROUTING_TELEPORTATION = """
<module name="routing" >
    <parameterset type="teleportedModeParameters">
        <param name="beelineDistanceFactor" value="1.3" />
        <param name="mode" value="bike" />
        <param name="teleportedModeSpeed" value="4.166666666666667" />
    </parameterset>
    <parameterset type="teleportedModeParameters">
        <param name="beelineDistanceFactor" value="1.3" />
        <param name="mode" value="walk" />
        <param name="teleportedModeSpeed" value="0.8333333333333333" />
    </parameterset>
    <parameterset type="teleportedModeParameters">
        <param name="beelineDistanceFactor" value="1.3" />
        <param name="mode" value="non_network_walk" />
        <param name="teleportedModeSpeed" value="0.8333333333333333" />
    </parameterset>
    <parameterset type="teleportedModeParameters">
        <param name="beelineDistanceFactor" value="1.3" />
        <param name="mode" value="ride" />
        <param name="teleportedModeFreespeedFactor" value="1.0" />
    </parameterset>
    <parameterset type="teleportedModeParameters">
        <param name="beelineDistanceFactor" value="1.3" />
        <param name="mode" value="pt" />
        <param name="teleportedModeFreespeedFactor" value="2.0" />
    </parameterset>
    <parameterset type="teleportedModeParameters">
        <!-- NOTE: drsRider routes are internally calculated with the drsDriver routing module.
                Unfortunately we still need to provide a teleportation config for drsRider
                because it may be used by the ReRoute strategy (even if the result of the rerouting does not make sense
                and will be replaced by the results of our matching algorithm immediately.)
                To specify a teleportation config for drsRider we need to also explicitly set the
                default modes, because they are deleted as soon as a single mode is configured here -->
        <param name="beelineDistanceFactor" value="1.3" />
        <param name="mode" value="drsRider" />
        <param name="teleportedModeFreespeedFactor" value="2.0" />
    </parameterset>
</module>
"""

LOG_FORMAT = "%(levelname)s | %(message)s"
logging.basicConfig(format=LOG_FORMAT, datefmt="%Y-%m-%d %H:%M:%S", level=logging.INFO)
logger = logging.getLogger(__name__)

# removing blank text is required for pretty printing to work
CLEAN_PARSER = etree.XMLParser(remove_blank_text=True, remove_comments=False)


def add_drs_module(tree: etree.ElementTree) -> None:
    if tree.find("module[@name='drs']") is not None:
        logger.info("drs: module config already present")
        return

    logger.info("drs: add module config")
    drs = etree.fromstring(DRS_MODULE, parser=CLEAN_PARSER)
    tree.getroot().insert(0, drs)


def adjust_qsim(tree: etree.ElementTree) -> None:
    modes = tree.find("module[@name='qsim']/param[@name='mainMode']")
    value = modes.get("value")
    if "drsDriver" in value:
        logger.info("qsim.mainMode: drsDriver already present")
        return
    logger.info("qsim.mainMode: add drsDriver")
    modes.set("value", f"{value},drsDriver")


def adjust_replanning(tree: etree.ElementTree) -> None:
    replanning = tree.find("module[@name='replanning']")
    removal_selector = replanning.find("param[@name='planSelectorForRemoval']")
    if removal_selector != "WorstPlanForRemovalSelectorWithConflicts":
        logger.warning(
            "replanning.planSelectorForRemoval: must take conflict resolving into account, "
            "otherwise ConflictManager is likely to stop the simulation with the exception '* has no non-conflicting plan'."
            "Verify your selector or switch to 'WorstPlanForRemovalSelectorWithConflicts'"
        )

    smc = tree.xpath(
        "module[@name='replanning']/"
        "parameterset[@type='strategysettings' and param[@name='strategyName'] and param[@value='SubtourModeChoice']]"
    )
    logger.info(f"replanning: using {len(smc)} SubtourModeChoice strategies")
    logger.warning(
        "TODO implement checking if drsDriver and drsRider are present in the subtourmodechoice config group"
    )


def adjust_routing(tree: etree.ElementTree) -> None:
    routing = tree.find("module[@name='routing']")
    modes = routing.find("param[@name='networkModes']")
    value = modes.get("value")
    if "drsDriver" in value:
        logger.info("routing.networkModes: drsDriver already present")
    else:
        logger.info("routing.networkModes: add drsDriver")
        modes.set("value", f"{value},drsDriver")

    logger.info("routing.clearDefaultTeleportedModeParams: set true")
    clear = routing.find("param[@name='clearDefaultTeleportedModeParams']")
    if clear:
        clear.set("value", "true")
    else:
        clear = etree.Element(
            "param", name="clearDefaultTeleportedModeParams", value="true"
        )
        routing.append(clear)

    teleport_params = routing.findall("parameterset[type='teleportedModeParameters']")
    if len(teleport_params) > 0:
        logger.info(
            f"routing.teleportedModeParameters: remove {len(teleport_params)} existing ones"
        )
        for p in teleport_params:
            p.getparent().remove(p)

    new_teleport_params = etree.fromstring(ROUTING_TELEPORTATION, parser=CLEAN_PARSER)
    logger.info(
        f"routing.teleportedModeParameters: add {len(new_teleport_params)} from template"
    )
    for p in etree.fromstring(ROUTING_TELEPORTATION, parser=CLEAN_PARSER):
        routing.append(p)


def adjust_scoring(tree: etree.ElementTree) -> None:
    logger.info("scoring: adjust general config (no subpops)..")
    _adjust_scoring(tree.find("module[@name='scoring']"))
    # or in case of subpopulations - for each scoringParameters
    for p in tree.findall(
        "module[@name='scoring']/parameterset[@type='scoringParameters']"
    ):
        subpop = p.find("param[@name='subpopulation']").get("value")
        logger.info(f"scoring: adjust subpopulation {subpop}..")
        _adjust_scoring(p)


def _adjust_scoring(scoring: etree.Element) -> None:
    mode_params = scoring.findall("parameterset[@type='modeParams']")
    if len(mode_params) == 0:
        logger.info("-> skip")
        return

    for p in mode_params:
        mode = p.find("param[@name='mode']").get("value")
        daily_cost = p.find("param[@name='dailyMonetaryConstant']")
        if daily_cost is None:
            continue

        daily_cost = daily_cost.get("value")
        if mode == "car" and float(daily_cost) != 0:
            logger.warning(
                f"scoring for 'car' sets a dailyMonetaryConstant of {daily_cost}. "
                "It should be 0 to avoid the agent paying dailyMonetaryCost twice in case a plan has both 'car' and 'drsDriver' legs. "
                "Consider using drs.carAndDrsDailyMonetaryConstant instead"
            )

    driver_mode_q = (
        "parameterset[@type='modeParams']/param[@name='mode'][@value='drsDriver']"
    )

    driver_mode_missing = len(scoring.findall(driver_mode_q)) == 0
    if driver_mode_missing:
        logger.info("-> add drsDriver modeParams")
        params = etree.fromstring(SCORING_MODEPARAMS_DRIVER, parser=CLEAN_PARSER)
        scoring.append(params)

    rider_mode_q = (
        "parameterset[@type='modeParams']/param[@name='mode'][@value='drsRider']"
    )
    rider_mode_missing = len(scoring.findall(rider_mode_q)) == 0
    if rider_mode_missing:
        logger.info("-> add drsRider modeParams")
        params = etree.fromstring(SCORING_MODEPARAMS_RIDER, parser=CLEAN_PARSER)
        scoring.append(params)

    activity_q = "parameterset[@type='activityParams']/param[@activityType='drsDriver interaction']"
    activity_missing = len(scoring.findall(activity_q)) == 0
    if activity_missing:
        logger.info("-> add drsDriver activityParams")
        params = etree.fromstring(SCORING_ACTIVITYPARAMS_DRIVER, parser=CLEAN_PARSER)
        scoring.append(params)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("in_file", type=Path)
    parser.add_argument("out_file", type=Path)
    args = vars(parser.parse_args())

    logger.info(f"reading config from {args["in_file"]}")
    tree = etree.parse(args["in_file"], CLEAN_PARSER)

    add_drs_module(tree)
    adjust_qsim(tree)
    adjust_replanning(tree)
    adjust_routing(tree)
    adjust_scoring(tree)

    tree.write(
        args["out_file"],
        encoding=tree.docinfo.encoding,
        pretty_print=True,
        xml_declaration=True,
    )
    logger.info(f"adjusted config written to {args["out_file"]}")
    logger.warning("make sure to address / double-check all warnings!")
