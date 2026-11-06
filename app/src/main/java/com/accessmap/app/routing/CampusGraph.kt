package com.accessmap.app.routing

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.*

enum class SurfaceType { PAVED, UNEVEN, GRAVEL, UNKNOWN }

data class CampusNode(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val hasRamp: Boolean = true,
    val stairCount: Int = 0,
    val hasElevator: Boolean = false,
    val isBuilding: Boolean = true,
    val accessibilityScore: Int = 8, // 1-10
    val description: String = ""
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
     * Steep slopes and uneven surfaces are heavily penalized but not blocked outright.
     */
    fun accessibilityCost(): Double {
        if (hasStairs || !wheelchairAccessible) return Double.POSITIVE_INFINITY
        var cost = distance
        cost += when {
            slope > 8.0 -> 90.0
            slope > 4.0 -> 20.0
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

    fun normalCost(): Double = distance
}

class CampusGraph(
    val nodes: List<CampusNode>,
    val edges: List<CampusEdge>
) {
    private val adjacency: Map<String, List<CampusEdge>> by lazy {
        val map = mutableMapOf<String, MutableList<CampusEdge>>()
        for (edge in edges) {
            map.getOrPut(edge.from) { mutableListOf() }.add(edge)
            // treat paths as bidirectional
            map.getOrPut(edge.to) { mutableListOf() }.add(
                edge.copy(from = edge.to, to = edge.from)
            )
        }
        map
    }

    fun neighbors(nodeId: String): List<CampusEdge> = adjacency[nodeId].orEmpty()
    fun nodeById(id: String): CampusNode? = nodes.find { it.id == id }
    val buildings: List<CampusNode> get() = nodes.filter { it.isBuilding }
}

/** Haversine distance in meters between two lat/lng coordinates */
fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0 // Earth radius in meters
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dPhi = Math.toRadians(lat2 - lat1)
    val dLambda = Math.toRadians(lon2 - lon1)
    val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

/**
 * Access point to the 47 surveyed LPU nodes and connected edges.
 * Supports loading from `assets/edges.csv` or fallback to built-in edge network.
 */
object LpuCampusData {

    val nodes: List<CampusNode> = LPU_CAMPUS_NODES

    // Built-in default edges connecting the 47 nodes
    val defaultEdges: List<CampusEdge> by lazy {
        val rawEdges = listOf(
            Triple("MAIN_GATE", "B1", false),
            Triple("MAIN_GATE", "B2", false),
            Triple("B1", "B2", false),
            Triple("B1", "B3", false),
            Triple("B2", "B3", false),
            Triple("B3", "B4", false),
            Triple("B4", "B7", false),
            Triple("B4", "B6", false),
            Triple("B6", "B8", true), // stairs between Architecture and Fine Arts
            Triple("B7", "B6", false),
            Triple("B8", "B15", false),
            Triple("B6", "B13", false),
            Triple("B8", "B9", false),
            Triple("B9", "B10", false),
            Triple("B10", "B11", false),
            Triple("B10", "B12", false),
            Triple("B11", "B15", false),
            Triple("B12", "B13", false),
            Triple("B13", "B15", false),
            Triple("B15", "B18", false),
            Triple("B18", "B19", false),
            Triple("B19", "B20", false),
            Triple("B20", "B21", false),
            Triple("B18", "B27", false),
            Triple("B15", "B29", false),
            Triple("B13", "B30", false),
            Triple("B13", "B31", false),
            Triple("B21", "B25", false),
            Triple("B25", "B26", false),
            Triple("B26", "B27", false),
            Triple("B27", "B28", false),
            Triple("B28", "B29", false),
            Triple("B29", "B30", false),
            Triple("B30", "B31", false),
            Triple("B31", "B32", false),
            Triple("B32", "B33", false),
            Triple("B33", "B34", false),
            Triple("B28", "LIB_ARC", false),
            Triple("B29", "LIB_ARC", false),
            Triple("B30", "LIB_ARC", false),
            Triple("B31", "LIB_ARC", false),
            Triple("B32", "LIB_ARC", false),
            Triple("B33", "B36", false),
            Triple("B34", "B36", false),
            Triple("B36", "LIB_ARC", false),
            Triple("LIB_ARC", "B37", false),
            Triple("B37", "B38", false),
            Triple("B38", "B27", false),
            Triple("B38", "B26", false),
            Triple("B25", "B41", false),
            Triple("B41", "B42", false),
            Triple("B42", "B43", false),
            Triple("B43", "B44", false),
            Triple("B41", "B47", false),
            Triple("B42", "B47", false),
            Triple("B44", "B45", false),
            Triple("B45", "B48", false),
            Triple("B48", "B49", false),
            Triple("B49", "B50", false),
            Triple("B48", "B51", false),
            Triple("B51", "B52", false),
            Triple("B52", "B53", false),
            Triple("B34", "B51", false),
            Triple("B36", "B51", false),
            Triple("B51", "B56", false),
            Triple("B52", "B56", false),
            Triple("B56", "B58", false),
            Triple("B58", "B57", false),
            Triple("B6", "B2", true) // alternate path with stairs
        )

        val nodeMap = nodes.associateBy { it.id }
        rawEdges.mapNotNull { (fromId, toId, hasStairs) ->
            val fromNode = nodeMap[fromId] ?: return@mapNotNull null
            val toNode = nodeMap[toId] ?: return@mapNotNull null
            val dist = haversineDistance(
                fromNode.latitude, fromNode.longitude,
                toNode.latitude, toNode.longitude
            )
            CampusEdge(
                from = fromId,
                to = toId,
                distance = dist,
                hasStairs = hasStairs,
                slope = if (hasStairs) 3.5 else 0.5,
                surface = if (hasStairs) SurfaceType.UNEVEN else SurfaceType.PAVED,
                wheelchairAccessible = !hasStairs
            )
        }
    }

    /**
     * Parse edges from assets/edges.csv or fall back to defaultEdges.
     */
    fun loadEdges(context: Context? = null): List<CampusEdge> {
        if (context == null) return defaultEdges
        return try {
            val list = mutableListOf<CampusEdge>()
            context.assets.open("edges.csv").use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    var isFirstLine = true
                    reader.forEachLine { line ->
                        if (isFirstLine) {
                            isFirstLine = false
                            return@forEachLine
                        }
                        val parts = line.split(",").map { it.trim() }
                        if (parts.size >= 7) {
                            val from = parts[0]
                            val to = parts[1]
                            val distance = parts[2].toDoubleOrNull() ?: 50.0
                            val hasStairs = parts[3].equals("true", ignoreCase = true)
                            val slope = parts[4].toDoubleOrNull() ?: 0.0
                            val surface = try {
                                SurfaceType.valueOf(parts[5].uppercase())
                            } catch (e: Exception) {
                                SurfaceType.PAVED
                            }
                            val accessible = parts[6].equals("true", ignoreCase = true)
                            list.add(
                                CampusEdge(
                                    from = from,
                                    to = to,
                                    distance = distance,
                                    hasStairs = hasStairs,
                                    slope = slope,
                                    surface = surface,
                                    wheelchairAccessible = accessible
                                )
                            )
                        }
                    }
                }
            }
            if (list.isNotEmpty()) list else defaultEdges
        } catch (e: Exception) {
            defaultEdges
        }
    }

    fun buildGraph(context: Context? = null): CampusGraph {
        return CampusGraph(nodes, loadEdges(context))
    }
}
