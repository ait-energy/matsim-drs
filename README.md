# Dynamic Ride-Sharing (DRS) for MATSim

This MATSim module (or extension) simulates dynamic ride-sharing (DRS),
i.e. it enables car drivers to pick up other agents,
share a ride with them in the same vehicle,
and drop them off before continuing their own trip.

In more detail DRS can be defined as follows:

- Drivers using a private car pick up riders for a part of their trip
- The driver's trip is part of the daily routine
  (not an additional trip with the sole purpose of picking up a rider)
- Driver and rider are not necessarily acquaintances
- Matching is facilitated through a platform (e.g. app) on a *trip-by-trip basis*
  (which defines the *dynamic* part of the DRS acronym)

**Related Work**:

Often passengers in cars have been incorporated as `ride` agents
teleported from one activity location to another using car travel times
and without being matched with other agents (e.g. Ziemke et al (2019)).
While this approach suffices for private ride-sharing,
it fails to represent the potential for DRS where non-acquaintances share rides to reach similar destinations.

Wang et al (2017) conducted previous endeavors to model DRS in MATSim,
employing the `dvrp`module [(source code)](https://github.com/strawrange/matsim/tree/master/contribs/dvrp).
Their approach involved introducing a dynamic agent alongside the standard population agent during simulation iterations,
specifically chosen when the leg's mode is ride share driver.

## Simulation of DRS

The implemented DRS has the following additional properties / restrictions:

- A *maximum of one rider* is allowed per DRS trip
- Pickup and dropoff of the rider takes place *directly at the rider's activity locations*
- Matching of drivers and riders is done *before the start of each day* (no spontaneous requests during the day or during the trip),
  i.e. in the `replanning` phase of the MATSim loop before `mobsim`.
  In comparison to Wang et al (2017) this approach reduces complexity and avoids waiting time for the rider,
  since with the `dvrp` module only allows a rider to request a ride when he already wants to depart.

**New modes of transport**

- `drsDriver`
- `drsRider`

**New person attribute**

Agents require the a priori attribute `drsAffinity` that determines if they are potential driver, rider, both or no DRS user.

- `drsAffinity` must be set for each person to either
  - `driverOrRider`
  - `driverOnly`
  - `riderOnly`
  - `none`

In more detail, DRS is integrated into the MATSim loop as follows:

### Replanning

First the slightly adjusted innovation strategy `SubtourModeChoiceForDrs` assigns the new modes `drsDriver` and `drsRider` to agents' subtours.
This assignment can be restricted with the optional person attribute `drsAffinity`.

Then requests are collected and matched based on origin, destination, departure time and detour time.
The riders' acceptance of deviations to their desired departure time can be controlled with the DRS config parameter `riderDepartureTimeAdjustmentSeconds`.
The matching algorithm ensures that all rider legs for an agent are matched (or none).

Then agents' plans are adjusted.
This entails adding pickup and dropoff activities at the riders' origins and destinations to the plans of matched drivers.
For matched riders the departure time is adjusted if necessary.

Finally, the conflict resolver switches to a valid plan for agents whose plan contains unmatched rider legs.

### Mobsim

Each matched driver proceeds to the specified pickup point to collect the assigned rider.
The DRS config parameter `pickupWaitingSeconds` determines how long a driver waits for a delayed rider before proceeding.
Subsequently, the driver transports the rider to the designated dropoff point.
Concurrently, matched riders await the arrival of their driver for pickup and subsequently for dropoff at the predetermined locations.

Note, that to successfully simulate DRS it is necessary to kickstart the pool of potential drivers.
Before the first iteration all agents that can potentially act as a driver are assigned a copy of their original plan with as many driver legs as possible.
This avoids the problem of riders not finding a match.

### Before next iteration

Adjustments to the agents' plans are reverted.

## Usage

### Installation

Releases and snapshots are available on
[repo.matsim.org](https://repo.matsim.org/service/rest/repository/browse/matsim/at/ac/ait/matsim/matsim-drs/).

To include the DRS module in your maven project,
add the MATSim repo and the following dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>matsim</id>
        <url>https://repo.matsim.org/repository/matsim/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>at.ac.ait.matsim</groupId>
        <artifactId>matsim-drs</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>
```

The DRS module's major version (e.g. 14) corresponds to the MATSim version it is compatible with.

### Config Parameters

[config_drs.xml](data/floridsdorf/config_drs.xml) serves as a complete example on how to configure a DRS scenario.
Note, that other modules must be configured in a specific way as well, e.g. `qsim`.

List of all parameters:

**Costs**

- `carAndDrsDailyMonetaryConstant`: Daily price for car usage including when using the private car as drsDriver.
  If specified here do not additionaly specify it in planCalcScore.scoringParameters.modeParams.dailyMonetaryConstant -
  otherwise it will be counted twice (typically negative)
- `driverProfitPerKm`: The amount of money per kilometre the driver gains for a rider (typically positive)
- Note, that for the **drsRider's costs** there is no parameter,
  because this should be set in `planCalcScore.modeParams.monetaryDistanceRate` (typically negative)

**Matching Algorithm**

- `cellSize`: The side length of square zones in meters used in zone registers of riders requests.
  The default value is good for urban areas. For large areas with sparsely distributed population and low drs share,
  you may consider using a bigger cell size. On the other hand, if neighbourhoodSize is very low, a smaller cell size may work better.
- `maxPossibleCandidates`: Limits the number of possible riders requests considered for a driver during the matching process.
  Used to speed up computations, values 20 to 40 make a good trade-off between computational speed and quality of results.
  To turn off this feature specify a sufficiently big number (not recommended).
- `minDriverLegMeters`: minimum length of legs (routed with the drsDriver mode) to be considered for the drs driver mode. 0 means no minimum.
- `minRiderLegMeters` minimum length of legs (routed with the drsDriver mode) to be considered for the drs ride mode. 0 means no minimum.
- `timeSegmentLengthSeconds`: The duration of the time segments used in time segment registers of riders requests.
  To avoid scenarios where a driver and a rider departure time are close but cross a segment boundary
  candidate requests are token not only from the current segment but also from the one before and after.

**Simulation / Plan Adjustment**

- `pickupWaitingSeconds` The amount of time the driver is expected to wait until the rider enters the vehicle.
- `riderDepartureTimeAdjustmentSeconds` The amount of time the riders are willing to adjust their departure times.
  During the matching process, the arrival of driver to pick-up point is checked
  whether it is within the rider departure time +- the riderDepartureTimeAdjustment.

**Plan Innovation**

- `subtourModeChoiceChainBasedModes`: Defines the chain-based modes for the `SubtourModeChoiceForDrs` strategy, separated by commas.
- `subtourModeChoiceModes`: Defines all modes available for the `SubtourModeChoiceForDrs` strategy, including chain-based modes, separated by commas.

### Run Example

The main method of [RunSimpleDrsExample](src/main/java/at/ac/ait/matsim/drs/run/RunSimpleDrsExample.java)
takes only the config file as an input in order to run.

- It automatically adds `drsDriver` mode as an allowed mode to all car links.
- It automatically kick-starts all potential `drsDriver` agents,
  i.e. with an according `drsAffinity` + car + license availability, with a `drsDriver` plan.

This should assure, that at the beginning of the simulation many drivers are present and "starvation" of the people choosing rider mode is avoided.
(MATSim guarantees to try out / score all un-scored plans of an agent - see `RandomUnscoredPlanSelector` -
before a different plan is selected e.g. via `SelectPlanExpBeta`).

### Mode Innovation

Mode innovation relies on an adapted version of the innovation strategy `SubtourModeChoice` named `SubtourModeChoiceForDrs`.
`SubtourModeChoiceForDrs` will by default add the DRS driver and rider mode to the mix
and can also be configured via the relevant parameters in the `drs` config group.

### Output

The following output files are additionally created in the MATSim output directory:

- `drs_rider_request_stats.csv/png`: evolution of number of matched and unmatched rider requests
- `drs_vkt_stats.csv/png`: evolution of distribution of vehicle kilometers traveled by motorized individual transport between
  - DRS travel, i.e. parts of a DRS driver's trip with an actual rider
  - before and after DRS, i.e. parts of a DRS driver's trip to the pickup point / after dropping off the rider
  - individual travel, i.e. regular non-DRS car trips or unmatched DRS driver trips
- `drs_[un]matched_trips.csv`: details for all matched and unmatched trips of the *last iteration*
- `drs_sim_stats.csv`: stats (e.g. nr of stuck riders) of the actual simulation run (similar to experienced plans)

## Limitations & Future Work

- Only **one rider is supported per driver's trip** (but a driver may take different riders along on different trips).
- If a `drsDriver` can not pick up a `drsRider` because it is not there it will still make the **detour** to the dropoff place.
  A future improvement is to optimize the drivers' route on the fly in such cases
- **Unmatched drivers and riders** even after reaching equilibrium account for 6% and 0.3% respectively for our Upper Austria model.
  Ideally that should be 0.
- **Improve the matching algorithm**
  - Option for predefined pickup points (instead of door-to-door)
  - Use sociodemographic attributes in the matching algorithm, not only for scoring
  - Avoid local optimums (and implement a more complex optimization algorithm)

## Literature

**Reference publications**

- Müller, J., Nassar, E., Straub, M., Chou, A. T. M., 2024.
  [*Integrating Dynamic Ride-Sharing into an Agent-Based Traffic Simulation: A Sensitivity Analysis*](https://www.researchgate.net/publication/376718010_Integrating_Dynamic_Ride-Sharing_into_an_Agent-Based_Traffic_Simulation_A_Sensitivity_Analysis).
  103rd Annual Meeting of the Transportation Research Board
- Nassar, E., 2023.
  [*Integrating Dynamic Ride-Sharing in MATSim*](https://www.mos.ed.tum.de/fileadmin/w00ccp/tb/theses/2023/Eyad_Masters_Thesis.pdf).
  Master’s thesis. Technical University Munich.

**Related**

- Wang, B., Liang, H., Hörl, S., Ciari, F., 2017.
  *Dynamic ride sharing implementation and analysis in matsim*. hEART 2017.
- Ziemke, D., Kaddoura, I., Nagel, K., 2019.
  *The matsim open berlin scenario: A multimodal agent-based transport simulation scenario based on synthetic demand modeling and open data*.
  Procedia computer science 151, 870–877.

## License & Credits

The DRS module is [GPL-2.0-or-later](LICENSE) licensed for maximum compatibility with [MATSim](https://github.com/matsim-org/matsim-libs).

This work was developed by Eyad Nassar for his master's thesis (Nassar, 2023) with support of Markus Straub and Johannes Müller.

It is part of the lead project [DOMINO](https://www.domino-maas.at/),
which was promoted and financed within the framework of the RTI programme *Mobility of the Future*
by the Federal Ministry for Climate Protection, Environment, Energy, Mobility, Innovation and Technology (BMK)
and handled by the Austrian Research Promotion Agency (FFG) under grant [3300226](https://projekte.ffg.at/projekt/3300226).

If you have any questions, remarks, or collaboration ideas, please get in touch:
either via GitHub or via email to `markus.straub` or `johannes.mueller` (both at `ait.ac.at`).