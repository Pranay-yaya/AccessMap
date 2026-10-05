package com.accessmap.app.routing

import java.util.PriorityQueue

data class RouteResult(
    val path: List<String>,       // ordered node ids
    val edges: List<CampusEdge>,  // edges traversed, in order
    val totalCost: Double
)

/**
 * Dijkstra over accessibility-weighted edges. Returns null if no accessible
 * path exists between the two nodes (e.g. only stairs connect them).
 */
class AccessibilityRouter(private val graph: CampusGraph) {

    fun findAccessibleRoute(startId: String, endId: String): RouteResult? {
        val distances = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
        val previous = mutableMapOf<String, CampusEdge>()
        val visited = mutableSetOf<String>()

        distances[startId] = 0.0
        val queue = PriorityQueue<String>(compareBy { distances.getValue(it) })
        queue.add(startId)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            if (current in visited) continue
            visited.add(current)
            if (current == endId) break

            for (edge in graph.neighbors(current)) {
                val edgeCost = edge.accessibilityCost()
                if (edgeCost.isInfinite()) continue // blocked: stairs or non-accessible

                val newDist = distances.getValue(current) + edgeCost
                if (newDist < distances.getValue(edge.to)) {
                    distances[edge.to] = newDist
                    previous[edge.to] = edge
                    queue.add(edge.to)
                }
            }
        }

        if (distances.getValue(endId).isInfinite()) return null // no accessible path found

        // reconstruct path
        val path = mutableListOf(endId)
        val edgesUsed = mutableListOf<CampusEdge>()
        var cursor = endId
        while (cursor != startId) {
            val edge = previous[cursor] ?: return null
            edgesUsed.add(edge)
            cursor = edge.from
            path.add(cursor)
        }
        path.reverse()
        edgesUsed.reverse()

        return RouteResult(path, edgesUsed, distances.getValue(endId))
    }
}
