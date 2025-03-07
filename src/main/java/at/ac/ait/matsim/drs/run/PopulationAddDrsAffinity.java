package at.ac.ait.matsim.drs.run;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.io.ParseException;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

import at.ac.ait.matsim.drs.util.DrsUtil;

public class PopulationAddDrsAffinity {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) throws ParseException {
        if (args.length != 2) {
            LOGGER.error("Exactly two arguments expected: input file, output file.");
            return;
        }

        String populationXml = args[0];
        String manipulatedPopulationXml = args[1];

        if (Files.exists(Paths.get(manipulatedPopulationXml))) {
            LOGGER.error("output file {} already exists.", manipulatedPopulationXml);
            return;
        }

        Population population = PopulationUtils.readPopulation(populationXml);
        LOGGER.info("read {} agents.", population.getPersons().size());

        int fixed = DrsUtil.addMissingDrsAffinity(population);
        if (fixed == 0) {
            LOGGER.info("All agents already had a {}, great!", Drs.ATTRIB_AFFINITY);
        } else {
            LOGGER.warn("For {} agents {} was missing and has been added.", fixed, Drs.ATTRIB_AFFINITY);
        }
        LOGGER.info("writing plans with drsAffinity for {} agents.", population.getPersons().size());
        PopulationUtils.writePopulation(population, manipulatedPopulationXml);
    }

}
