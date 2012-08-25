PdxExplorers
============

This plugin allows users to configure routes for other players to complete.
Routes are designated with specially formatted signs. Routes must be completed
without use of teleportation, death, or flying.

Signs use the following format:

[explorer]
COMMAND
ROUTE NAME
WINNERS

The valid commands are:

  * Start - Starting point for a route
  * Waypoint - Intermediate waypoint
  * Finish - Finish point for a route
  * View - View all the players who have finished this route
  * Lock - Lock the block underneath this sign to be used by players who have finished the route
  * Enroute - Lock the block underneath this sign to be used by players who are currently using this route

The names of the players who have completed a route will be automatically displayed in a rotation at the
bottom of every command sign.

Using Waypoints
===============

Waypoints allow an arbitrary number of intermediate checkpoints along a route. The Waypoint and Finish commands
take an extra number argument to identify the exact waypoint they represent. A example of a valid sequence is

  * Start
  * Waypoint: 1
  * Waypoint: 2
  * Finish: 3

Commands
========

/explorers
----------
Display the player's current exploration progress.

/explorers routes
-----------------
Display the names of all of the routes.

/explorers players
-----------------
Display the names of all players currently on a route.

/explorers assign PLAYERNAME [ROUTENAME [WAYPOINT]]
---------------------------------
Manually assign a player's progress on a route and waypoint. The waypoint defaults to 0 and the route defaults to none.

/explorers route delete ROUTENAME
---------------------------------
Delete all information about the given route.

/explorers route revoke ROUTENAME PLAYERNAME
---------------------------------
Delete completion status for a player for a route

/explorers route show ROUTENAME
---------------------------------
Display the owner of a route, number of winners, and rewards for completing the route.

/explorers route addreward ROUTENAME MATERIAL QUANTITY
---------------------------------
Add the given material in the given quantity as a reward for completing the given route.
/explorers route winners ROUTENAME
---------------------------------
Display the list of players who have completed this route.
