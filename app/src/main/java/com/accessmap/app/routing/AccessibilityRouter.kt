package com.accessmap.app.routing

import java.util.PriorityQueue

data class RouteResult(
    val path: List<String>,
    val edges: List<CampusEdge>,
    val totalCost: Double,
    val totalDistanceMeters: Double,
    val avoidedHazards: Boolean = false
)

class AccessibilityRouter(private val graph: CampusGraph) {

    fun findAccessibleRoute(
        startId: String,
        endId: String,
        blockedNodeIds: Set<String> = emptySet()
    ): RouteResult? {
        if (blockedNodeIds.isNotEmpty()) {
            val hazardFree = dijkstra(startId, endId, accessible = true, blockedNodeIds = blockedNodeIds)
            if (hazardFree != null) return hazardFree.copy(avoidedHazards = true)
        }
        return dijkstra(startId, endId, accessible = true, blockedNodeIds = emptySet())
    }

    fun findNormalRoute(
        startId: String,
        endId: String,
        blockedNodeIds: Set<String> = emptySet()
    ): RouteResult? {
        if (blockedNodeIds.isNotEmpty()) {
            val hazardFree = dijkstra(startId, endId, accessible = false, blockedNodeIds = blockedNodeIds)
            if (hazardFree != null) return hazardFree.copy(avoidedHazards = true)
        }
        return dijkstra(startId, endId, accessible = false, blockedNodeIds = emptySet())
    }

    private fun dijkstra(
        startId: String,
        endId: String,
        accessible: Boolean,
        blockedNodeIds: Set<String>
    ): RouteResult? {
        val distances = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
        val previous = mutableMapOf<String, CampusEdge>()
        val visited = mutableSetOf<String>()

        distances[startId] = 0.0
        val queue = PriorityQueue<String>(compareBy { distances.getValue(it) })
        queue.add(startId)

        while (queue.isNotEmpty()) {
            val current = queue.poll() ?: break
            if (current in visited) continue
            visited.add(current)
            if (current == endId) break

            for (edge in graph.neighbors(current)) {
                // Skip edges leading into an active hazard (unless it's the destination itself)
                if (edge.to != endId && edge.to in blockedNodeIds) continue

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
        val edges = mutableListOf<CampusEdge>()
        var curr = endId
        while (curr != startId) {
            val edge = previous[curr] ?: break
            edges.add(edge)
            curr = edge.from
            path.add(curr)
        }
        path.reverse()
        edges.reverse()

        val totalDist = edges.sumOf { it.distance }
        return RouteResult(path, edges, distances.getValue(endId), totalDist)
    }
}
