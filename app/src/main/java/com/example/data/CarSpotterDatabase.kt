package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CarSpotEntity::class], version = 1, exportSchema = false)
abstract class CarSpotterDatabase : RoomDatabase() {
    abstract fun carSpotDao(): CarSpotDao

    companion object {
        @Volatile
        private var INSTANCE: CarSpotterDatabase? = null

        fun getInstance(context: Context): CarSpotterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarSpotterDatabase::class.java,
                    "carspotter_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default sample cars asynchronously
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).carSpotDao().insertSpots(initialSampleCars())
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun initialSampleCars(): List<CarSpotEntity> {
            val now = System.currentTimeMillis()
            return listOf(
                CarSpotEntity(
                    id = "spot_porsche_gt3rs",
                    make = "Porsche",
                    model = "911 GT3 RS",
                    year = 2024,
                    trim = "Weissach Package",
                    rarityTier = "EXOTIC",
                    rarityScore = 92,
                    pointsAwarded = 850,
                    horsepower = 518,
                    zeroToSixtyMph = 3.0,
                    topSpeedMph = 184,
                    estimatedPriceUsd = 241300,
                    engineType = "4.0L Naturally Aspirated Flat-6",
                    drivetrain = "RWD • 7-Speed PDK",
                    description = "The ultimate track weapon with DRS rear wing, central radiator, and aerodynamic downforce rivaling full race cars.",
                    trivia = "Generates 1,895 lbs of downforce at 177 mph—triple that of a standard GT3.",
                    photoUri = "res:car_porsche",
                    locationName = "Angeles Crest Highway, CA",
                    latitude = 34.2384,
                    longitude = -118.0645,
                    timestamp = now - 3600000 * 4,
                    isAuthentic = true,
                    antiCheatConfidence = 0.99f,
                    antiCheatDetails = "Verified authentic sighting: Natural direct sunlight, physical asphalt reflections, and genuine depth-of-field.",
                    isFirstDiscovery = true
                ),
                CarSpotEntity(
                    id = "spot_ferrari_296gtb",
                    make = "Ferrari",
                    model = "296 GTB",
                    year = 2024,
                    trim = "Assetto Fiorano",
                    rarityTier = "EXOTIC",
                    rarityScore = 95,
                    pointsAwarded = 950,
                    horsepower = 819,
                    zeroToSixtyMph = 2.9,
                    topSpeedMph = 205,
                    estimatedPriceUsd = 342000,
                    engineType = "3.0L Twin-Turbo 120° V6 + Plug-in Electric",
                    drivetrain = "RWD • 8-Speed F1 Dual-Clutch",
                    description = "Maranello's mid-rear-engined berlinetta that introduces revolutionary 120° wide-angle turbo architecture.",
                    trivia = "Nicknamed the 'piccolo V12' (little V12) for its harmonic, operatic exhaust note.",
                    photoUri = "res:car_ferrari",
                    locationName = "Rodeo Drive, Beverly Hills, CA",
                    latitude = 34.0697,
                    longitude = -118.4055,
                    timestamp = now - 3600000 * 26,
                    isAuthentic = true,
                    antiCheatConfidence = 0.98f,
                    antiCheatDetails = "Verified authentic sighting: Authentic street perspective, pedestrian scale coherence, and real curb shadows.",
                    isFirstDiscovery = true
                )
            )
        }
    }
}
