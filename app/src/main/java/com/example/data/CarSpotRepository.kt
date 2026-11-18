package com.example.data

import com.example.model.CarSpot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CarSpotRepository(private val dao: CarSpotDao) {

    fun getAllSpotsFlow(): Flow<List<CarSpot>> {
        return dao.getAllSpotsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addSpot(spot: CarSpot) {
        dao.insertSpot(CarSpotEntity.fromDomain(spot))
    }

    suspend fun deleteSpot(spot: CarSpot) {
        dao.deleteSpot(CarSpotEntity.fromDomain(spot))
    }
}
