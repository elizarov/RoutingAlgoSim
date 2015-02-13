Routing Algorithms Simulator
============================

Routing algorithms simulator with UI display that shows behaviour of different distributed routing
algorithms for directed graphs. It also contain stress-testing code to test correctness of all
presented algorithms.

DFB: Distributed Ford-Bellman Algorithm
---------------------------------------

A classic. Count-to-infinity is avoided by limiting distance to 2000.

MDVA: A Distance-Vector Multi-path Routing Protocol
---------------------------------------------------

This is an implementation of the algorithm described in paper
"MDVA: A Distance-Vector Multi-path Routing Protocol"
by Srinivas Vutukury, J.J. Garcia-Luna-Aceves.

The algorithm is modified for directed link graph. Links can change distance in each direction separately,
however when link it dropped, it must be dropped in both directions at the same time.

MDVAm: A Distance-Vector Multi-path Routing Protocol (modified)
---------------------------------------------------------------

An additional feature of this implementation is that in addition to MDVA it maintains a routing graph that
does not temporarily breakdown on link disappearance (as does MDVA successor DAG), but choose to retain
outing information from last known distances. The trade off is that this routing graph can have
temporary routing loops.

DPV: Distance + Path Vector Algorithm
-------------------------------------

This is an implementation of a distance + path vector algorithm. It is a straightforward extension of DFB
with a set of intermediate nodes on the shortest paths for loop detection.

It is optimized to avoid extra messages. The loops are detected before sending messages to neighbouring nodes,
so that neighbours are receiving INF metric from a link that got disconnected from the alternative shorter
paths to the destination.

SPTA: Shortest Path Topology Algorithm
--------------------------------------

This is an implementation of the algorithm described in paper
"Broadcasting Topology Information in Computer Networks" by John M. Spinelli Robert G. Gallag.
The algorithm is modified for directed link graph.

