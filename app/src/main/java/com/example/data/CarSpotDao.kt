package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CarSpotDao {
    @Query("SELECT * FROM car_spots ORDER BY timestamp DESC")
    fun getAllSpotsFlow(): Flow<List<CarSpotEntity>>

    @Query("SELECT * FROM car_spots WHERE id = :id LIMIT 1")
    suspend fun getSpotById(id: String): CarSpotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: CarSpotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpots(spots: List<CarSpotEntity>)

    @Update
    suspend fun updateSpot(spot: CarSpotEntity)

    @Delete
    suspend fun deleteSpot(spot: CarSpotEntity)

    @Query("SELECT COUNT(*) FROM car_spots")
    suspend fun getCount(): Int

    @Query("SELECT SUM(pointsAwarded) FROM car_spots")
    fun getTotalPointsFlow(): Flow<Int?>
}
