package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart School Life", appName)
  }

  @Test
  fun testSchoolSearch() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.SecurityUtil.initialize(context)
    try {
      val results = com.example.data.remote.NetworkClients.searchSchool(context, "서울고")
      println("TEST_LOG_SUCCESS: Results count = ${results.size}")
      results.forEach { println("TEST_LOG_SCHOOL: ${it.schulNm} (${it.sdSchulCode})") }
    } catch (e: Exception) {
      println("TEST_LOG_ERROR: Failed to search school")
      e.printStackTrace()
    }
  }

  @Test
  fun testPreheatData() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.SecurityUtil.initialize(context)
    val app = context.applicationContext as android.app.Application
    val viewModel = com.example.ui.viewmodel.MainViewModel(app)
    
    val cfg = com.example.data.local.SchoolConfig(
      id = 1,
      schulNm = "서울고등학교",
      atptOfcdcScCode = "B10",
      sdSchulCode = "7010083",
      schoolKind = "고등학교",
      grade = "1",
      classNm = "1",
      isFirstLaunch = false
    )
    
    viewModel.configRepo.saveConfig(cfg)
    
    // Fetch and insert directly for the whole month
    viewModel.mealRepo.refreshMeals(context, cfg.atptOfcdcScCode, cfg.sdSchulCode, "20260601", "20260630", true)
    viewModel.timetableRepo.refreshTimetable(context, cfg.atptOfcdcScCode, cfg.sdSchulCode, cfg.schoolKind, cfg.grade, cfg.classNm, "20260601", "20260630", true)
    
    val db = com.example.data.local.AppDatabase.getDatabase(context)
    val meals = db.mealDao().getMealsForDate("20260609").first()
    val timetable = db.timetableDao().getTimetableForDate("20260609").first()
    val allTimetable = db.timetableDao().getTimetableForRange("20260601", "20260630").first()
    
    println("TEST_MEALS_COUNT: ${meals.size}")
    meals.forEach { println("TEST_MEAL: ${it.mealType} - ${it.dishes}") }
    
    println("TEST_TIMETABLE_COUNT: ${timetable.size}")
    timetable.forEach { println("TEST_PERIOD: ${it.perio} - ${it.subject}") }
    println("TEST_TOTAL_TIMETABLE_IN_DB: ${allTimetable.size}")
  }

  @Test
  fun testDecryptNeisKey() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.SecurityUtil.initialize(context)
    com.example.util.SecurityUtil.withNeisKey(context) { key ->
      println("TEST_NEIS_KEY_DECRYPTED: '$key'")
    }
  }

  @Test
  fun testSaveCustomNeisKey() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.SecurityUtil.initialize(context)
    
    // Check initial state
    assertEquals(false, com.example.util.SecurityUtil.hasCustomNeisKey(context))
    
    // Save custom key
    com.example.util.SecurityUtil.saveCustomNeisKey(context, "MY_TEST_CUSTOM_NEIS_API_KEY_123")
    assertEquals(true, com.example.util.SecurityUtil.hasCustomNeisKey(context))
    
    // Retrieve custom key
    com.example.util.SecurityUtil.withNeisKey(context) { key ->
      assertEquals("MY_TEST_CUSTOM_NEIS_API_KEY_123", key)
    }
    
    // Reset custom key
    com.example.util.SecurityUtil.saveCustomNeisKey(context, "PUBLIC")
    assertEquals(false, com.example.util.SecurityUtil.hasCustomNeisKey(context))
    
    com.example.util.SecurityUtil.withNeisKey(context) { key ->
      assertEquals("a3c352bb8c1d4549b8272b75870991dd", key)
    }
  }
}
