package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CarSpotEntity::class], version = 1, exportSchema = false)
abstract class CarSpotterDatabase : RoomDatabase() {
    abstract fun carSpotDao(): CarSpotDao

    companion object {
        private const val LEGACY_DB_NAME = "carspotter_db"

        @Volatile
        private var INSTANCE: CarSpotterDatabase? = null

        /**
         * Purges any un-scoped local Room database from prior app versions so user garages
         * remain strictly tied to authenticated Cloud Firestore accounts.
         */
        fun clearLegacyLocalGarage(context: Context) {
            try {
                context.applicationContext.deleteDatabase(LEGACY_DB_NAME)
            } catch (_: Exception) {
            }
        }

        fun getInstance(context: Context): CarSpotterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarSpotterDatabase::class.java,
                    LEGACY_DB_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
