package at.ac.ait.matsim.domino.carpooling.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;

import javax.inject.Inject;

public class CarpoolingScoringFunctionFactory implements ScoringFunctionFactory {

	private final Scenario scenario;
	private final ScoringParametersForPerson params;
	@Inject
    CarpoolingScoringFunctionFactory(final Scenario sc) {
		this.scenario = sc;
		this.params = new SubpopulationScoringParameters( sc );
	}


	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction scoringFunctionSum = new SumScoringFunction();
		scoringFunctionSum.addScoringFunction(
	    new CarpoolingScoringFunction( params.getScoringParameters( person ), this.scenario.getNetwork()));
		scoringFunctionSum.addScoringFunction(
				new CharyparNagelLegScoring(
						params.getScoringParameters( person ),
						this.scenario.getNetwork(),
						this.scenario.getConfig().transit().getTransitModes())
			    );
			scoringFunctionSum.addScoringFunction(
				new CharyparNagelActivityScoring(
						params.getScoringParameters(
								person ) ) );
		scoringFunctionSum.addScoringFunction(
				new CharyparNagelAgentStuckScoring(
						params.getScoringParameters(
								person ) ) );
	    return scoringFunctionSum;
	  }
}
