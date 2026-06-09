package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "school_config")
data class SchoolConfig(
    @PrimaryKey val id: Int = 1,
    val atptOfcdcScCode: String = "",
    val sdSchulCode: String = "",
    val schulNm: String = "",
    val schoolKind: String = "", // e.g. "초등학교", "중학교", "고등학교"
    val grade: String = "",
    val classNm: String = "",
    val isFirstLaunch: Boolean = true,
    val themeMode: String = "SYSTEM" // "SYSTEM", "LIGHT", "DARK"
)

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val id: String, // date_mealType format (e.g. "20260609_중식")
    val date: String, // YYYYMMDD
    val mealType: String, // "조식", "중식", "석식"
    val dishes: String,
    val calories: String = "",
    val nutrition: String = "",
    val origins: String = "",
    val aiDescription: String? = null,
    val imageUrl: String? = null
)

@Entity(tableName = "timetable")
data class TimetableEntity(
    @PrimaryKey val id: String, // date_perio format (e.g. "20260609_1")
    val date: String, // YYYYMMDD
    val perio: Int, // 1, 2, 3...
    val subject: String
)

@Entity(tableName = "school_events")
data class SchoolEventEntity(
    @PrimaryKey val id: String, // date_eventName format
    val date: String, // YYYYMMDD
    val eventName: String,
    val eventContent: String = ""
)

@Entity(tableName = "custom_events")
data class CustomEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYYMMDD
    val title: String,
    val time: String = "", // e.g. "15:00"
    val syncedToSystem: Boolean = false,
    val systemEventId: Long? = null
)

@Entity(tableName = "timetable_memos")
data class TimetableMemoEntity(
    @PrimaryKey val id: String, // date_perio
    val date: String, // YYYYMMDD
    val perio: Int,
    val memo: String
)
