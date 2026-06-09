package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SchoolConfig::class,
        MealEntity::class,
        TimetableEntity::class,
        SchoolEventEntity::class,
        CustomEventEntity::class,
        TimetableMemoEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolConfigDao(): SchoolConfigDao
    abstract fun mealDao(): MealDao
    abstract fun timetableDao(): TimetableDao
    abstract fun schoolEventDao(): SchoolEventDao
    abstract fun customEventDao(): CustomEventDao
    abstract fun timetableMemoDao(): TimetableMemoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_school_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
