package at.ac.ait.matsim.domino.carpooling.scoring;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.functions.ScoringParameters;



public class CarpoolingScoringFunction extends org.matsim.core.scoring.functions.CharyparNagelLegScoring {

	public CarpoolingScoringFunction(ScoringParameters params, Network network) {
		super(params, network);
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);		
	}	
	
	@Override
	public void finish() {		
		super.finish();
	}	
	
	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {

		return departureTime;
	}

}
