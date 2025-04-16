# Efficient Turf

A Java library/tool to calculate the route between two Turf zones which maximizes earned points, while not exceeding the given time limit.

This is a rewrite/remake of my older project [Efficient Turf v1](https://github.com/icicel/efficient-turf), written in Python.

### What's Turf?

(from [the official Turf website](https://turfgame.com/))  
Turf is a location-based game using GPS technology, which requires you to go to the physical location of the virtual zones in order to take them.
Try to take as many zones as you can to rack up points, earn medals, level up and climb the leaderboards!

### Why bother?

Sometimes, I have been in the situation where I have one or two hours of free time and I decide to take a walk and play some Turf.
I then face the dilemma where I want to get as many points as possible in order to maximize my time efficiency, but I also want to make it back on time.
In order to help solve this conundrum and generate the optimal route for the walk, this tool exists.

# How to use

In order to generate a route, Turf zones are treated as nodes on a graph, with edges (**connections**) having weights according to their length in the real world.
Of course, Turf zones and the streets between them don't correspond as well to a simple graph as we'd like.
Luckily, we can also define **crossings** - custom, non-point giving, purely functional zones - to keep the amount of edges in control.

## Importing data

Zones, crossings and connections are all imported from KML files with a specific format.
Such files can be easily generated using [Google My Maps](https://www.google.com/maps/d/).

(Note: My Maps is no longer maintained by Google.
A more modern approach to making the KML file would be [Google Earth](https://earth.google.com).
The steps should be similar.)

In My Maps, create one layer for zones, one for crossings (optionally) and one for connections.
Make sure each layer has a unique name.
Then, create zones and crossings using **markers** and define connections using **lines**.
Afterwards, the entire map can be exported with:  
**⋮** *→ Export to KML/KMZ → Export as KML instead of KMZ → Download*

Tip: Using [the Turf Map Tool](https://turf.urbangeeks.org/) you can import Turf zones directly to My Maps. 
Draw a polygon containing all relevant zones, and then *Download polygon as → KML - POIs* (or *KML - Zone polygons* for the actual zone shapes).
Then in My Maps you can either 1. create a new layer and import this KML file into it, or 2. in an already existing layer, go to *Layer options → Reimport and merge → Add more items* and import the KML there.

The example KML corresponds to [this map of southern Stenungsund, Sweden](https://www.google.com/maps/d/u/0/edit?mid=1iv00_Yvkj4J3LrPsgByOfHsf2090pNg).

## Objects

The basic process consists of three steps.
These are also outlined in the example files in the `test` folder, containing two use cases.

Use `Logging.init` to enable debug output.

### `Turf`

The raw zone and connection information (the Turf map) is stored in a Turf object.
The constructor takes
1. The path to the KML file containing the map information.
It's assumed to have a format consistent with KMLs exported by the method above.
2. The names of the relevant layers in that KML file - one for zones, one for connections and (optionally) one for crossings.

The KML is scanned for zone and connection data, and the Turf API is accessed for zone points values.
All this is stored in the Turf object using **Zone** and **Connection** objects.

Note that Zone objects are not the same as actual Turf zones.
Crossings are defined internally as Zone objects with a point value of zero.
This means that actual zones are counted as crossings if they give zero points (that is, if you own them and the revisit timer hasn't passed yet).

### `Conditions`

The Conditions object contains various problem-specific information.
The only required information that can be given is the start zone, the end zone, and the time limit; these are the three arguments taken by the constructor.

The key to this step is that multiple different Conditions can be created, while keeping only one underlying Turf object.
This means we can skip reimporting map data that has already been imported.

### `Scenario`

Applying a Conditions object onto a Turf object, a Scenario is created.
This represents the complete problem that needs solving, and is the "final simplification" of the graph.

Scenarios use **Node** and **Link** objects to store map data instead of Zones and Connections.
Links are one-way, and thus a Connection turns into two Links.

The Scenario graph can optionally be further optimized by taking into account an important fact: we only want to travel on the fastest possible routes between zones, and we want to ignore all routes that would bring us anywhere else.
The fastest route between two zones is called the "direct route" for short.

The optimizations offered are:
- `removeUnusedConnections` - Removes all crossings and connections that don't lie on any direct route.
- (WIP)

## Solvers

A Scenario can be solved using any implementation of Solver.
Simply call `Solver.solve` and pass the Scenario as the argument.
It returns a Result, which is is a list of routes and can be read and printed in various ways.

Below is a list of all Solvers that have currently been implemented.

### `BruteForceSolver`

This is the original solving algorithm, from v1.
It simply tries every possible route, skipping routes that are guaranteed to be worse than a potential other route.
(This can happen, for example, when the route follows a non-direct path between any two of its zones.
This guarantees that there is a better route, namely the one that takes the direct path.)

While the original was breadth-first search, this implementation is depth-first.
