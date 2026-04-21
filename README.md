# Efficient Turf

A Java library/tool to calculate the route between two Turf zones which maximizes earned points, while not exceeding the given time limit.

This is a rewrite/remake of my older project [Efficient Turf v1](https://github.com/icicel/efficient-turf), written in Python.

### What's Turf?

(from [the official Turf website](https://turfgame.com/))  
Turf is a location-based game using GPS technology, which requires you to go to the physical location of the virtual zones in order to take them.
Try to take as many zones as you can to rack up points, earn medals, level up and climb the leaderboards!

### Background

Sometimes, I have been in the situation where I have one or two hours of free time and I decide to take a walk and play some Turf.
I then face the dilemma where I want to get as many points as possible in order to maximize my time efficiency, but I also want to make it back on time.
In order to help solve this conundrum and generate the optimal route for the walk, this tool exists.

Technically, this problem is called the [Orienteering Problem](https://www.sciencedirect.com/topics/mathematics/orienteering-problem) and is NP-hard.
If the time limit is infinite, then it's equivalent to the far better known [Traveling Salesman Problem](https://en.wikipedia.org/wiki/Travelling_salesman_problem).

# How to use

In order to generate a route, Turf zones are treated as nodes on a graph, with edges (**connections**) having weights according to their length in the real world.
Of course, Turf zones and the streets between them don't correspond as well to a simple graph as we'd like.
Luckily, we can also define **crossings** - custom, non-point giving, purely functional zones - to keep the amount of edges in control.

## Importing data

Zones are imported with [the Turf Map Tool](https://turf.urbangeeks.org/). 
Draw a polygon containing all relevant zones, and then download it as *Zones within polygon → KML - POIs*.
This will either be put into a larger KML file or be combined with an OpenStreetMap network depending on your import method of choice.

### KML

Crossings and connections can be defined using a KML file with map information.
Such files can be easily created using [Google My Maps](https://www.google.com/maps/d/).

(Note: My Maps is no longer maintained by Google.
A more modern approach to making the KML file would be [Google Earth](https://earth.google.com).
The steps should be similar.)

In My Maps, create one layer each for zones, crossings and connections, making sure each layer has a unique name.
Import your zones into their layer by uploading the KML file containing them.
Then, define connections using **lines** and create crossings using **markers** as needed.
Afterwards, the entire map can be exported with:  
**⋮** *→ Export to KML/KMZ → Export as KML instead of KMZ → Download*

Note that if a zone has an invalid name, the program will fail.
This may happen if they are defined manually.

To get the actual Turf zone shapes as a reference, you can use the Turf Map Tool like above.
Just download the polygon as *Zones within polygon → KML - Zone polygons* instead.

There is an example KML created using this method in the repository root.
It corresponds to [this My Maps file of southern Stenungsund, Sweden](https://www.google.com/maps/d/u/0/edit?mid=1iv00_Yvkj4J3LrPsgByOfHsf2090pNg).

### OSM

Crossings and connections can be defined using an XML file with OpenStreetMap network data.
This can be extracted using the [Overpass API](https://wiki.openstreetmap.org/wiki/Overpass_API).
All intersections between ways become crossings and between them are connections.

Since Overpass can be somewhat bulky, extraction can be handled by the program.
It calculates the bounding box of all zones (specifically, the max/min coordinates among the zones in both directions) and downloads all walkable roads within it.
If you want to extract this data yourself, you can of course also supply an XML file directly and override this process.

A downside of this method is that OSM coverage, especially for footpaths and outside of major cities, may be unreliable.
In the case of missing paths, you could map the information yourself, upload it to OSM and it would eventually appear in the export.
This is not always possible, though, e.g. a shortcut across a field shouldn't really appear on the map even if it is technically walkable.

## Objects

The basic process consists of three steps - creating the objects Turf, Conditions and Scenario in that order.
These are outlined in the example files in the `test` folder, containing two use cases.

Use `Logging.init` to enable debug output for all objects.

### `Turf`

The raw zone and connection information (the Turf map) is stored in a Turf object.
The constructor takes, depending on import method:
- A KML file and the names of the zone, connection and (optionally) crossing layers in it
- A KML file with zones and (optionally) an XML file with OSM network data

Map information is stored in the Turf object using **Point** and **Connection** objects.
For Points that correspond to Turf zones (which may be all of them), the program accesses the Turf API for the zone data and stores it in **Zone** objects.

Points and Connections retain their map data and can be exported back to KML format using `Export`.

If based on OSM data, the Turf graph could get quite large, easily reaching 25k objects.
To this end, there are two compression methods available: `Turf.compress` and `Turf.optimize`, with the latter being the more aggressive option.

### `Conditions`

The Conditions object contains various problem-specific information.
The bare minimum information that can be given is the start zone, the end zone, and the time limit; these are the three arguments taken by the constructor.
Optional conditions that can be given are:
- Walking speed (defaults to 1 m/s)
- Turf username, for zone ownership calculations
- Whether to consider zone ownership at all
- Blacklisting of nodes
- Greylisting of zones (conversion into crossings)
- Whitelisting of nodes, a.k.a. a reverse blacklist

The key to this step is that multiple different Conditions can be created, while keeping only one underlying Turf object.
This means we can skip reimporting map data that has already been imported.

### `Scenario`

Applying a Conditions object onto a Turf object, a Scenario is created.
This represents the complete problem that needs solving, and is the "final simplification" of the graph.
As such, it does not represent crossings at all (unless they are the start/end), only zones.

Scenarios use **Node** and **Link** objects to store map data instead of Points and Connections.
Links are one-way, so a Connection turns into two Links.

## Solvers

A Scenario can be solved using any implementation of Solver.
Simply call `Solver.solve` and pass the Scenario as the argument along with an optional time limit.
This returns a Result, which is is a list of Routes that can be read and printed.

Below is a list of all Solvers that have currently been implemented.

### `BruteForceSolver`

This is the original solving algorithm, from v1.
It simply tries every possible route, skipping routes that are guaranteed to be worse than a potential other route or otherwise unfinishable.

While the original was breadth-first search, this implementation is depth-first.

### `GreedySolver`

A variant of BruteForceSolver that attempts to "direct" its search towards the nearest zones, in order to find the best route quicker.
Whenever a new best route is found, it is printed to the output.
