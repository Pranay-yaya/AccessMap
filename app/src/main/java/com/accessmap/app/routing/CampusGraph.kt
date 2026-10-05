package com.accessmap.app.routing

enum class SurfaceType { PAVED, UNEVEN, GRAVEL, UNKNOWN }

data class CampusNode(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class CampusEdge(
    val from: String,
    val to: String,
    val distance: Double,          // meters
    val hasStairs: Boolean,
    val slope: Double,             // degrees
    val surface: SurfaceType,
    val wheelchairAccessible: Boolean
) {
    /**
     * Cost used by the router. Stairs make an edge unusable for accessible routing.
     * Steep slopes and uneven surfaces are heavily penalized but not blocked outright,
     * so the router can still fall back to them if nothing better exists.
     */
    fun accessibilityCost(): Double {
        if (hasStairs || !wheelchairAccessible) return Double.POSITIVE_INFINITY

        var cost = distance
        cost += when {
            slope > 8.0 -> 90.0   // steep incline penalty
            slope > 4.0 -> 20.0   // moderate incline penalty
            else -> 0.0
        }
        cost += when (surface) {
            SurfaceType.UNEVEN -> 140.0
            SurfaceType.GRAVEL -> 60.0
            SurfaceType.UNKNOWN -> 30.0
            SurfaceType.PAVED -> 0.0
        }
        return cost
    }
}

/**
 * Simple in-memory campus graph. Later this can be backed by Room instead of
 * hardcoded lists once you've surveyed real paths on campus.
 */
class CampusGraph(
    val nodes: List<CampusNode>,
    val edges: List<CampusEdge>
) {
    private val adjacency: Map<String, List<CampusEdge>> by lazy {
        val map = mutableMapOf<String, MutableList<CampusEdge>>()
        for (edge in edges) {
            map.getOrPut(edge.from) { mutableListOf() }.add(edge)
            // treat paths as bidirectional unless you model one-way routes separately
            map.getOrPut(edge.to) { mutableListOf() }.add(
                edge.copy(from = edge.to, to = edge.from)
            )
        }
        map
    }

    fun neighbors(nodeId: String): List<CampusEdge> = adjacency[nodeId].orEmpty()
}
