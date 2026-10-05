package com.accessmap.app.repository

import com.accessmap.app.database.HazardDao
import com.accessmap.app.database.HazardEntity
import com.accessmap.app.routing.AccessibilityRouter
import com.accessmap.app.routing.CampusGraph
import com.accessmap.app.routing.RouteResult
import kotlinx.coroutines.flow.Flow

class AccessMapRepository(
    private val hazardDao: HazardDao,
    private val campusGraph: CampusGraph
) {
    private val router = AccessibilityRouter(campusGraph)

    fun activeHazards(): Flow<List<HazardEntity>> = hazardDao.getActiveHazards()

    suspend fun reportHazard(hazard: HazardEntity): Long = hazardDao.insert(hazard)

    suspend fun resolveHazard(id: Long) = hazardDao.markResolved(id)

    fun findRoute(startNodeId: String, endNodeId: String): RouteResult? =
        router.findAccessibleRoute(startNodeId, endNodeId)
}
