package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolConfigDao {
    @Query("SELECT * FROM school_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<SchoolConfig?>

    @Query("SELECT * FROM school_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): SchoolConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: SchoolConfig)

    @Query("UPDATE school_config SET isFirstLaunch = :isFirst WHERE id = 1")
    suspend fun updateFirstLaunch(isFirst: Boolean)
}

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE date = :date")
    fun getMealsForDate(date: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE date >= :startDate AND date <= :endDate")
    fun getMealsForRange(startDate: String, endDate: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :id LIMIT 1")
    suspend fun getMealById(id: String): MealEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>)

    @Query("DELETE FROM meals WHERE date = :date")
    suspend fun deleteMealsForDate(date: String)
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable WHERE date = :date ORDER BY perio ASC")
    fun getTimetableForDate(date: String): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, perio ASC")
    fun getTimetableForRange(startDate: String, endDate: String): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, perio ASC")
    suspend fun getTimetableForRangeDirect(startDate: String, endDate: String): List<TimetableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetable(timetableList: List<TimetableEntity>)

    @Query("DELETE FROM timetable WHERE date = :date")
    suspend fun deleteTimetableForDate(date: String)

    @Query("DELETE FROM timetable WHERE date >= :startDate AND date <= :endDate")
    suspend fun deleteTimetableForRange(startDate: String, endDate: String)
}

@Dao
interface SchoolEventDao {
    @Query("SELECT * FROM school_events WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getEventsForRange(startDate: String, endDate: String): Flow<List<SchoolEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<SchoolEventEntity>)

    @Query("DELETE FROM school_events WHERE date >= :startDate AND date <= :endDate")
    suspend fun deleteEventsForRange(startDate: String, endDate: String)
}

@Dao
interface CustomEventDao {
    @Query("SELECT * FROM custom_events WHERE date = :date ORDER BY time ASC")
    fun getCustomEventsForDate(date: String): Flow<List<CustomEventEntity>>

    @Query("SELECT * FROM custom_events WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, time ASC")
    fun getCustomEventsForRange(startDate: String, endDate: String): Flow<List<CustomEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomEvent(event: CustomEventEntity): Long

    @Query("DELETE FROM custom_events WHERE id = :id")
    suspend fun deleteCustomEvent(id: Int)

    @Query("UPDATE custom_events SET syncedToSystem = :synced WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, synced: Boolean)

    @Query("SELECT * FROM custom_events WHERE id = :id LIMIT 1")
    suspend fun getCustomEventById(id: Int): CustomEventEntity?
}

@Dao
interface TimetableMemoDao {
    @Query("SELECT * FROM timetable_memos WHERE date = :date")
    fun getMemosForDate(date: String): Flow<List<TimetableMemoEntity>>

    @Query("SELECT * FROM timetable_memos WHERE date >= :startDate AND date <= :endDate")
    fun getMemosForRange(startDate: String, endDate: String): Flow<List<TimetableMemoEntity>>

    @Query("SELECT * FROM timetable_memos WHERE id = :id LIMIT 1")
    suspend fun getMemoById(id: String): TimetableMemoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: TimetableMemoEntity)

    @Query("DELETE FROM timetable_memos WHERE id = :id")
    suspend fun deleteMemo(id: String)
}
