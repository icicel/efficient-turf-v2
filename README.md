# Efficient Turf

A Java library/tool to calculate the route between two Turf zones which maximizes earned points, while not exceeding the given time limit.

This is a rewrite/remake of my older project [Efficient Turf v1](https://github.com/icicel/efficient-turf), originally written in Python.
A rewrite was done to untangle the spaghetti code of the original.

### What's a Turf?

(from [the official Turf website](https://turfgame.com/))  
Turf is a location-based game using GPS technology, which requires you to go to the physical location of the virtual zones in order to take them.
Try to take as many zones as you can to rack up points, earn medals, level up and climb the leaderboards!

## Basics

Sometimes, I have been in the situation where I have one or two hours of free time and I decide to take a walk and play some Turf.
I then face the dilemma where I want to get as many points as possible in order to maximize my time efficiency, but I also want to make it back on time.
In order to help solve this conundrum, this tool exists.

In order to generate a route, Turf zones are treated as nodes on a graph, with edges (**connections**) having weights according to their length in the real world.
Of course, Turf zones and the streets between them don't correspond as well to a simple graph as we'd like.
Luckily, we can also define **crossings** - custom, non-point giving, purely functional zones - to keep the amount of edges in control.

## Importing data

Zones, crossings and connections are all imported from KML files with a specific format.
Such files can be easily generated using [Google My Maps](https://www.google.com/maps/d/).

In My Maps, create one layer for zones, one for crossings (optionally) and one for connections.
Make sure each layer has a unique name.
Then, create zones and crossings using **markers** and define connections using **lines**.
Afterwards, the entire map can be exported with:  
**⋮** *→ Export to KML/KMZ → Export as KML instead of KMZ → Download*

Tip: Using [the Turf Map Tool](https://turf.urbangeeks.org/) you can import Turf zones directly to My Maps. 
Draw a polygon containing all relevant zones, and then *Download polygon as → KML - POIs* (or *KML - Zone polygons* for the actual zone shapes).
Then in My Maps you can either 1. create a new layer and import this KML file into it, or 2. in an already existing layer, go to *Layer options → Reimport and merge → Add more items* and import the KML there.

The example KML corresponds to [this map of southern Stenungsund, Sweden](https://www.google.com/maps/d/u/0/edit?mid=1iv00_Yvkj4J3LrPsgByOfHsf2090pNg).

## How to use

The basic process consists of three steps.

### Create the baseline

The baseline (common variables) is stored in a Turf object.
The object only contains raw zone and connection information, and doesn't contain information such as the time limit or the start or end zone.
That information will be given later.

The Turf constructor takes four arguments. 
`kmlPath` is a path to a KML file that is assumed to have a format consistent with KMLs exported by the method above. 
`realZoneLayer`, `crossingLayer` and `connectionLayer` are the names of the relevant layers in that KML file. 
The KML is scanned for zone and connection data, and the Turf API is accessed for zone points values.
All this is stored in the Turf using Zone and Connection objects.

### Define the variant

A Conditions object is now created, which contains various problem-specific customization.
The absolute minimum information that can be given is the start zone, the end zone, and the time limit.
These are also the three arguments taken by the constructor.
All other variables can be optionally changed/defined.

- `speed` defines the simulated "Turfing speed", in m/min.
  It is 60 by default.
- `waitTime` defines how long to wait at every zone in minutes, a.k.a. the time it takes to take the zone.
  It is 1 by default.
- `username` is your username.
  It is used to take zone ownership into account when calculating how many points they are worth.
- `infiniteRounds` is a bool which defines if zone point calculation should ignore the fact that zone ownership is reset at the end of every round.
- `blacklist` is the names of all zones that should be ignored completely, including connections to and from them.
- `whitelist` is the same as `blacklist`, but every zone that ISN'T in it is ignored (unless the array is null, of course).
- `priority` is the names of zones that must be visited.
  Take care when using this; if too many zones are prioritized there may be no valid route to visit them all within the time limit.

### Solve the problem

Lastly, a Scenario is created from the Turf object and the Conditions.
This represents the complete problem that needs solving.
To emphasize the difference, Scenarios use Node and Link objects to store its zone information instead of Zones and Connections.
It's the "final simplification" of the graph, in a way.

WIP

### Structural notes

Forcing users to pipeline the process like this by adding the unnecessary Conditions/Scenario step was intentional.
I wanted to be able to quickly create different problem variants based on the same underlying zone data, without having to reimport everything for every new problem variant.

## Implemented solvers

This is a list of all Solvers that have currently been implemented.

WIP
