package com.accessmap.app.repository

import com.accessmap.app.database.AppDatabase
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

    fun activeHazards(): Flow<List<HazardEntity>> {
        val cutoff = System.currentTimeMillis() - AppDatabase.EXPIRY_MILLIS
        return hazardDao.getActiveHazards(cutoff)
    }

    suspend fun activeHazardsOnce(): List<HazardEntity> {
        val cutoff = System.currentTimeMillis() - AppDatabase.EXPIRY_MILLIS
        hazardDao.deleteExpired(cutoff)
        return hazardDao.getActiveHazardsOnce(cutoff)
    }

    suspend fun reportHazard(hazard: HazardEntity): Long = hazardDao.insert(hazard)

    suspend fun confirmStillThere(id: Long) = hazardDao.confirmStillThere(id, System.currentTimeMillis())

    suspend fun resolveHazard(id: Long) = hazardDao.markResolved(id)

    fun findRoute(
        startNodeId: String,
        endNodeId: String,
        blockedNodeIds: Set<String> = emptySet()
    ): RouteResult? = router.findAccessibleRoute(startNodeId, endNodeId, blockedNodeIds)
}
