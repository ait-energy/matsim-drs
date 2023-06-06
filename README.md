# Dynamic Ride-Sharing for MATSim

This extension simulates dynamic ride-sharing (DRS), i.e. agents driving a private car pick up riders for a part of their trip.
The assignment of DRS driver to rider is done before each QSim iteration.

The simulated modes of transport are:
- `drsDriver`
- `drsRider`

## Main features

The main components of this extension are:

1. Matching algorithm.
2. Modifying agents plans.
3. Handling agents departures and arrivals in QSim.
4. Undoing modifications in agents' plans.

### Matching Algorithm

Drivers and riders requests are created for agents who are using DRS.
The matching algorithm looks for the best rider for each driver and consists of the following steps:

1. Registering riders requests in zones in a zonal system (for both origin and destination) according to the requests origins and destinations.
2. Registering riders requests in time segments according to the requests departure times.
3. Finding riders requests that lay within same origin zone, destination zone and departure time segment of the driver.
4. Filtering out riders which departure times are not within driver arrival time to pickup point +- `riderDepartureTimeAdjustment`.
5. Finding out the rider with the least detour factor.

### Modifying agents plans

- In case of a matched driver: Two new interaction activities with the pickup and dropoff info will be added to the driver's plan.
- In case of a matched rider: Rider departure time will be adjusted to the driver's expected arrival time at pickup point.

### Handling agents departures and activities in QSim

- In case rider has a match, rider will be picked up by the right driver and drop off when arriving at the dropoff point.
- In case rider doesn't have a match, rider will be teleported.
- In case driver has a match, driver will pickup the right rider from the pickup point and drop him off at the dropoff point and then drives to his next activity.
- In case driver doesn't have a match, driver will drive directly to his next activity.

### Undoing modifications in agents' plans  at the end of each iteration

- Undoing the changes of rider's plan by restoring the original departure time of rider's activity.
- Undoing the changes of driver's plan by removing the extra plan elements added to the plan.

### Population preparation

- Attribute `drsAffinity` needs to be added for each agent. It can be either `driverOrRider`, `driverOnly`, `riderOnly` or `none`.

### Config preparation

- `config_drs.xml` serves as an example on how to correctly configure DRS.

## Usage

Main method of the `RunSimpleDrsExample` class takes only the config file as an input in order to run the DRS extension.

- It automatically adds `drsDriver` mode as an allowed mode to all car links.
- It automatically kick-starts all potential `drsDriver` agents, i.e. with an according `drsAffinity` + car + license availability, with a `drsDriver` plan. 

This should assure, that at the beginning of the simulation many drivers are present and "starvation" of the people choosing rider mode is avoided.
(MATSim guarantees to try out / score all un-scored plans of an agent - see `RandomUnscoredPlanSelector` - before a different plan is selected e.g. via `SelectPlanExpBeta`).

### Mode innovation

Mode innovation relies on an adapted version of the innovation strategy `SubtourModeChoice` named `SubtourModeChoiceForDrs`.
`SubtourModeChoiceForDrs` will by default add the DRS driver and rider mode to the mix and can also be configured via the relevant parameters in the `drs` config group.

## Output

- `drs_rider_request_stats.txt/png`
- `drs_vkt_stats.txt/png` 
- `drs_[un]matched_trips.csv` can be used for spatial analysis, temporal analysis and trip purpose analysis 

## Limitations

- Only one rider is supported per a driver's leg (but a driver may have different riders on different legs).
- If a `drsDriver` can not pick up a `drsRider` because it is not there it will still go to the dropoff place. A future improvement is to optimize the drivers route on the fly in such cases.
- `pickupWaitingSeconds` prolongs pickups until the calculated end time (even if the rider arrives before)

## Credits

The lead project [DOMINO](https://www.domino-maas.at/) was promoted and financed within the framework of the RTI programme Mobility of the Future by the Federal Ministry for Climate Protection, Environment, Energy, Mobility, Innovation and Technology (BMK) and handled by the [Austrian Research Promotion Agency](https://projekte.ffg.at/projekt/3300226).

Das Leitprojekt [DOMINO](https://www.domino-maas.at/) wurde gefördert bzw. finanziert im Rahmen des FTI-Programms Mobilität der Zukunft durch das Bundesministerium für Klimaschutz, Umwelt, Energie, Mobilität, Innovation und Technologie (BMK) und von der [Österreichischen Forschungsförderungsgesellschaft](https://projekte.ffg.at/projekt/3300226) abgewickelt.

## License

This extension is [GPL-2.0-or-later](LICENSE) licensed for maximum compatibility with [MATSim](https://github.com/matsim-org/matsim-libs).