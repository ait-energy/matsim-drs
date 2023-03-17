# Carpooling

## Main features

This extension simulates dynamic carpooling where carpooling agents including both drivers and riders send their requests before the day starts.

The simulated modes of transport are:
- `carpoolingDriver`
- `carpoolingRider`

In the population file, agents who are willing to pickup other agents should be given
carpoolingDriver mode and those who are willing to ride should be given carpoolingRider mode.

The main components of this extension are the following:

1. Matching algorithm
2. Modifying agents plans before each iteration
3. Handling agents departures and arrivals in QSim
4. Scoring carpooling trips
5. Undoing modifications in agents' plans at the end of each iteration

### Matching Algorithm
Before each iteration, drivers and riders requests are created for agents who are carpooling.
The matching algorithm looks for the best rider for each driver and consists of the following steps:

1. Registering riders requests in zones in a zonal system (for both origin and destination) according to the requests origins and destinations.
2. Registering riders requests in time segments according to the requests departure times.
3. Finding riders requests that lay within same origin zone, destination zone and departure time segment of the driver.
4. Filtering out riders which departure times are not within driver arrival time to pickup point +- `riderDepartureTimeAdjustment`.
5. Filtering out riders which would lead to detourFactor higher than `maxDetourFactor`.
6. Finding out the rider with the least detour factor.

### Modifying agents plans before each iteration
- In case of a matched driver: Two new interaction activities with the pickup and dropoff info will be added to the driver's plan.
- In case of a matched rider: Rider departure time will be adjusted to the driver's expected arrival time to pickup point.

### Handling agents departures and arrivals in QSim
- Riders wait until they get picked up by the right driver and drop off when arriving at the dropoff point.
- Driver pickup the right rider from the pickup point and drop him off in the dropoff point and then drives to his next activity.

### Scoring carpooling trips
.......................................................................................

### Undoing modifications in agents' plans  at the end of each iteration
- Undoing the changes of rider's plan by restoring the original departure time of rider's activity.
- Undoing the changes of driver's plan by removing the extra plan elements added to the plan.

## Usage

Main method of the `RunSimpleCarpoolingExample` class takes only the config file as an input in order to run the carpooling extension.

## Output

Output files are located in each iteration folder and contain the following information:
........................................................................................


## Credits

The lead project [DOMINO](https://www.domino-maas.at/) was promoted and financed within the framework of the RTI programme Mobility of the Future by the Federal Ministry for Climate Protection, Environment, Energy, Mobility, Innovation and Technology (BMK) and handled by the [Austrian Research Promotion Agency](https://projekte.ffg.at/projekt/3300226).

Das Leitprojekt [DOMINO](https://www.domino-maas.at/) wurde gefördert bzw. finanziert im Rahmen des FTI-Programms Mobilität der Zukunft durch das Bundesministerium für Klimaschutz, Umwelt, Energie, Mobilität, Innovation und Technologie (BMK) und von der [Österreichischen Forschungsförderungsgesellschaft](https://projekte.ffg.at/projekt/3300226) abgewickelt.
