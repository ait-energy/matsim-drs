# Matching Algorithm for Pickup @ Meeting Point (Mobility Hub)

**given (stable)**
- meeting points (coords, matched to multiple links) -> maybe needs to be narrowed down to one link per meeting point

**given (differs per iteration)**
- driver requests (A, B, departure time)
- rider requests (A, B, departure time)

## Algorithm

- we must look at complete subtours
- **vehicle continuity**:
  - HOME -car- HUB -drsRider- WORK -
  - WORK -drsRider- HUB -car- HOME
  - assumption: only allow hubs directly next to home (makes sense if access is car!): `HOME - HUB - ...` and `... - HUB - HOME`
  - assumption: for other trips in the subtour always `pt` is used
  - -> we need to use **DiscreteModeChoice** to do this?
    1. DMC sets the base modes (drsRider/Driver for the first and last trip, `pt` for all trips inbetween)
    2. our logic collects rider requests (storing if they should start or end with the ride mode) and solves the problem.


    ---> how did we solve the problem of partial plans not matching??

### Simplest

- access mode to hub is always `car` (in line with project INTRO)
- for `drsRider`: assign nearest hub to each agent
- for `drsDriver`: assign nearest hub to each agent (or maybe all hubs along the route to the destination - no alternative routes)
- ==ignore vehicle continuity (!?)== through use of SubtourModeChoice

- only solve for a really small scenario? e.g. one hub.. and then just try all routes..

### Ideas for more complex matching

- use `DiscreteModeChoice` to solve the vehicle continuity
- allow other access modes
- `drsDriver` should potentially pick up from more than one hub
- `drsDriver` should potentially pick up from all hubs near the route

### optimization approach

==potential further discussions with uritzinger, binhu, gbrandstätter (building on DOMINO stuff)==

## Else

**to think about...**
- current request collection does not retain subtour info?? (see vehicle continuity)
- switch to `DiscreteModeChoice`, potentially also with the additional stuff from `equasim`?