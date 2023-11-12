# Efficient Turf

A Java library/tool to calculate the route between two Turf zones which maximizes earned points, while not exceeding the given time limit.

This is a rewrite/remake of my older project [Efficient Turf v1](https://github.com/icicel/efficient-turf), originally written in Python.
A rewrite was done to untangle the spaghetti code of the original.

### What's a Turf?

(from [the official Turf website](https://turfgame.com/))  
Turf is a location-based game using GPS technology, which requires you to go to the physical location of the virtual zones in order to take them.
Try to take as many zones as you can to rack up points, earn medals, level up and climb the leaderboards!

## Basics

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

The basic process consists of three steps, corresponding to the Turf class, the Conditions and Scenario classes, and the Solver classes. 
First some common variables are defined, then problem-specific variables are added, and then the defined problem is solved.

Note that forcing the user to pipeline the process like this (in particular, differentiating between common and problem-specific variables) was intentional. 
I wanted to be able to quickly create different problem variants based on the same underlying zone data, without having to reimport everything for every new problem variant.

### Create the baseline

The baseline, the common variables, are stored in a Turf object.
The object only contains raw zone and connection information, and doesn't contain information such as the time limit or the start or end zone.
This information will instead be given later on in the pipeline.

The Turf constructor takes four arguments. 
`kmlPath` is a path to a KML file that is assumed to have a format consistent with KMLs exported by the method above. 
`realZoneLayer`, `crossingLayer` and `connectionLayer` are simply the names of the relevant layers in that KML file. 
The KML is scanned for zone and connection data, and the Turf API is accessed for zone points values.

### Define the variant

WIP

### Solve the problem

WIP

## Implemented solvers

This is a list of all Solvers that have currently been implemented.

WIP
