package com.accessmap.app.routing

import java.util.PriorityQueue

data class RouteResult(
    val path: List<String>,
    val edges: List<CampusEdge>,
    val totalCost: Double,
    val totalDistanceMeters: Double
)

class AccessibilityRouter(private val graph: CampusGraph) {

    fun findAccessibleRoute(startId: String, endId: String): RouteResult? =
        dijkstra(startId, endId, accessible = true)

    fun findNormalRoute(startId: String, endId: String): RouteResult? =
        dijkstra(startId, endId, accessible = false)

    private fun dijkstra(startId: String, endId: String, accessible: Boolean): RouteResult? {
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
                val edgeCost = if (accessible) edge.accessibilityCost() else edge.normalCost()
                if (edgeCost.isInfinite()) continue

                val newDist = distances.getValue(current) + edgeCost
                if (newDist < distances.getValue(edge.to)) {
                    distances[edge.to] = newDist
                    previous[edge.to] = edge
                    queue.add(edge.to)
                }
            }
        }

        if (distances.getValue(endId).isInfinite()) return null

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

        val totalDist = edgesUsed.sumOf { it.distance }
        return RouteResult(path, edgesUsed, distances.getValue(endId), totalDist)
    }
}
