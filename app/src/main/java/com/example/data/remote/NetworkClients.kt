package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.MealEntity
import com.example.data.local.SchoolConfig
import com.example.data.local.SchoolEventEntity
import com.example.data.local.TimetableEntity
import com.example.util.SecurityUtil
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object NetworkClients {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val NEIS_BASE_URL = "https://open.neis.go.kr/hub"

    /**
     * Searches school information from NEIS API.
     */
    fun searchSchool(context: Context, schoolName: String): List<SchoolConfig> = SecurityUtil.withNeisKey(context) { neisKey ->
        val encodedName = URLEncoder.encode(schoolName, "UTF-8")
        val keyParam = if (neisKey != "PUBLIC" && neisKey.isNotEmpty()) "&KEY=$neisKey" else ""
        val url = "$NEIS_BASE_URL/schoolInfo?Type=json&pIndex=1&pSize=50&SCHUL_NM=$encodedName$keyParam"
        
        val request = Request.Builder().url(url).build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withNeisKey emptyList()
                val json = JSONObject(bodyStr)
                if (!json.has("schoolInfo")) return@withNeisKey emptyList()
                
                val schoolInfoArray = json.getJSONArray("schoolInfo")
                if (schoolInfoArray.length() < 2) return@withNeisKey emptyList()
                val rowObject = schoolInfoArray.getJSONObject(1)
                val rows = rowObject.getJSONArray("row")
                
                val results = mutableListOf<SchoolConfig>()
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val kind = row.optString("SCHUL_KND_SC_NM", "")
                    // Only high, middle, elementary are supported
                    if (kind.contains("초등학교") || kind.contains("중학교") || kind.contains("고등학교")) {
                        results.add(
                            SchoolConfig(
                                atptOfcdcScCode = row.getString("ATPT_OFCDC_SC_CODE"),
                                sdSchulCode = row.getString("SD_SCHUL_CODE"),
                                schulNm = row.getString("SCHUL_NM"),
                                schoolKind = kind,
                                isFirstLaunch = false
                            )
                        )
                    }
                }
                return@withNeisKey results
            }
        } catch (e: Exception) {
            Log.e("NEIS_API", "Error searching school", e)
            return@withNeisKey emptyList()
        }
    }

    /**
     * Fetches meal diet from NEIS.
     */
    fun fetchMeals(
        context: Context,
        officeCode: String,
        schoolCode: String,
        fromDate: String,
        toDate: String
    ): List<MealEntity> = SecurityUtil.withNeisKey(context) { neisKey ->
        val keyParam = if (neisKey != "PUBLIC" && neisKey.isNotEmpty()) "&KEY=$neisKey" else ""
        val url = "$NEIS_BASE_URL/mealServiceDietInfo?Type=json&pIndex=1&pSize=100" +
                "&ATPT_OFCDC_SC_CODE=$officeCode&SD_SCHUL_CODE=$schoolCode" +
                "&MLSV_FROM_YMD=$fromDate&MLSV_TO_YMD=$toDate$keyParam"
                
        val request = Request.Builder().url(url).build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withNeisKey emptyList()
                val json = JSONObject(bodyStr)
                if (!json.has("mealServiceDietInfo")) return@withNeisKey emptyList()
                
                val mealArray = json.getJSONArray("mealServiceDietInfo")
                if (mealArray.length() < 2) return@withNeisKey emptyList()
                val rowObject = mealArray.getJSONObject(1)
                val rows = rowObject.getJSONArray("row")
                
                val meals = mutableListOf<MealEntity>()
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val date = row.getString("MLSV_YMD")
                    val type = row.getString("MMEAL_SC_NM")
                    
                    // Dishes contains HTML breaks like rice<br/>soup<br/>, let's clean them gently
                    val rawDishes = row.getString("DDISH_NM")
                    val cleanDishes = rawDishes
                        .replace("<br/>", ", ")
                        .replace("<br>", ", ")
                        .replace(Regex("\\([0-9.]+\\)"), "") // strip allergy codes like (1.2.5)
                        .replace(Regex("\\s+"), " ")
                        .trim()
                        
                    val calories = row.optString("CAL_INFO", "")
                    val nutrition = row.optString("NTR_INFO", "").replace("<br/>", "\n").replace("<br>", "\n")
                    val origins = row.optString("ORPLC_INFO", "").replace("<br/>", "\n").replace("<br>", "\n")
                    
                    val entity = MealEntity(
                        id = "${date}_$type",
                        date = date,
                        mealType = type,
                        dishes = cleanDishes,
                        calories = calories,
                        nutrition = nutrition,
                        origins = origins
                    )
                    meals.add(entity)
                }
                return@withNeisKey meals
            }
        } catch (e: Exception) {
            Log.e("NEIS_API", "Error fetching meals", e)
            return@withNeisKey emptyList()
        }
    }

    /**
     * Fetches timetable entries from NEIS.
     */
    fun fetchTimetable(
        context: Context,
        officeCode: String,
        schoolCode: String,
        schoolKind: String,
        grade: String,
        classNm: String,
        fromDate: String,
        toDate: String
    ): List<TimetableEntity> = SecurityUtil.withNeisKey(context) { neisKey ->
        // Decide appropriate endpoint: elsTimetable (elem), misTimetable (middle), hisTimetable (high)
        val endpoint = when {
            schoolKind.contains("고등학교") -> "hisTimetable"
            schoolKind.contains("중학교") -> "misTimetable"
            else -> "elsTimetable"
        }
        
        val keyParam = if (neisKey != "PUBLIC" && neisKey.isNotEmpty()) "&KEY=$neisKey" else ""
        val url = "$NEIS_BASE_URL/$endpoint?Type=json&pIndex=1&pSize=100" +
                "&ATPT_OFCDC_SC_CODE=$officeCode&SD_SCHUL_CODE=$schoolCode" +
                "&GRADE=$grade&CLASS_NM=$classNm" +
                "&TI_FROM_YMD=$fromDate&TI_TO_YMD=$toDate$keyParam"
                
        val request = Request.Builder().url(url).build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withNeisKey emptyList()
                val json = JSONObject(bodyStr)
                if (!json.has(endpoint)) return@withNeisKey emptyList()
                
                val tableArray = json.getJSONArray(endpoint)
                if (tableArray.length() < 2) return@withNeisKey emptyList()
                val rowObject = tableArray.getJSONObject(1)
                val rows = rowObject.getJSONArray("row")
                
                val list = mutableListOf<TimetableEntity>()
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val date = row.getString("ALL_TI_YMD")
                    val perio = row.getString("PERIO").toIntOrNull() ?: (i + 1)
                    val subject = row.getString("ITRT_CNTNT").trim()
                    
                    if (subject.isNotEmpty()) {
                        list.add(
                            TimetableEntity(
                                id = "${date}_${perio}_${subject}",
                                date = date,
                                perio = perio,
                                subject = subject
                            )
                        )
                    }
                }
                println("TEST_NETWORK_TIMETABLE_COUNT: ${list.size}")
                return@withNeisKey list
            }
        } catch (e: Exception) {
            Log.e("NEIS_API", "Error fetching timetable", e)
            return@withNeisKey emptyList()
        }
    }

    /**
     * Fetches official school academic calendar events.
     */
    fun fetchSchoolEvents(
        context: Context,
        officeCode: String,
        schoolCode: String,
        fromDate: String,
        toDate: String
    ): List<SchoolEventEntity> = SecurityUtil.withNeisKey(context) { neisKey ->
        val keyParam = if (neisKey != "PUBLIC" && neisKey.isNotEmpty()) "&KEY=$neisKey" else ""
        val url = "$NEIS_BASE_URL/SchoolSchedule?Type=json&pIndex=1&pSize=100" +
                "&ATPT_OFCDC_SC_CODE=$officeCode&SD_SCHUL_CODE=$schoolCode" +
                "&AA_FROM_YMD=$fromDate&AA_TO_YMD=$toDate$keyParam"
                
        val request = Request.Builder().url(url).build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withNeisKey emptyList()
                val json = JSONObject(bodyStr)
                if (!json.has("SchoolSchedule")) return@withNeisKey emptyList()
                
                val scheduleArray = json.getJSONArray("SchoolSchedule")
                if (scheduleArray.length() < 2) return@withNeisKey emptyList()
                val rowObject = scheduleArray.getJSONObject(1)
                val rows = rowObject.getJSONArray("row")
                
                val events = mutableListOf<SchoolEventEntity>()
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val date = row.getString("AA_YMD")
                    val name = row.getString("EVENT_NM").trim()
                    val content = row.optString("EVENT_CNTNT", "").trim()
                    
                    if (name.isNotEmpty() && name != "토요휴업일") {
                        events.add(
                            SchoolEventEntity(
                                id = "${date}_$name",
                                date = date,
                                eventName = name,
                                eventContent = content
                            )
                        )
                    }
                }
                return@withNeisKey events
            }
        } catch (e: Exception) {
            Log.e("NEIS_API", "Error fetching school schedule", e)
            return@withNeisKey emptyList()
        }
    }

    /**
     * Scrapes the first food image URL from DuckDuckGo image search.
     */
    fun searchDdgImage(query: String): String? {
        try {
            val encodedQuery = URLEncoder.encode("$query 음식", "UTF-8")
            
            // Step 1: Request DDG main page to get 'vqd' token
            val tokenRequest = Request.Builder()
                .url("https://duckduckgo.com/?q=$encodedQuery")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
                
            val tokenData = okHttpClient.newCall(tokenRequest).execute().use { res ->
                res.body?.string() ?: ""
            }
            
            val vqdMatch = """vqd\s*=\s*['"]([^'"]+)['"]""".toRegex().find(tokenData)
            val vqd = vqdMatch?.groupValues?.get(1) ?: return null
            
            // Step 2: Query DDG image endpoint JS
            val imageRequest = Request.Builder()
                .url("https://duckduckgo.com/i.js?q=$encodedQuery&o=json&vqd=$vqd")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()
                
            val jsonResponse = okHttpClient.newCall(imageRequest).execute().use { res ->
                res.body?.string() ?: ""
            }
            
            // Extract the first "image" parameter
            val imageMatch = """"(?:image)"\s*:\s*"([^"]+)"""".toRegex().find(jsonResponse)
            val scrapedUrl = imageMatch?.groupValues?.get(1)?.replace("\\", "")
            
            // Verify if we actually got a high-res image link
            if (scrapedUrl != null && scrapedUrl.startsWith("http")) {
                return scrapedUrl
            }
        } catch (e: Exception) {
            Log.e("DDG_SCRAPE", "Error scouting DDG image for $query", e)
        }
        return null
    }

    /**
     * Generates food nutritional analysis and fun facts using Gemini (gemini-3.5-flash).
     * Decrypts key, submits payload, and clears immediately.
     */
    fun fetchGeminiMealAnalysis(context: Context, mealDescription: String): String? {
        return SecurityUtil.withGeminiKey(context) { geminiKey ->
            if (geminiKey.isEmpty() || geminiKey == "MY_GEMINI_API_KEY") {
                val dishes = mealDescription.split(", ")
                val mainDish = dishes.firstOrNull() ?: "오늘의 급식"
                return@withGeminiKey "🌱 [영양소&맛] $mainDish 조합으로 영양이 골고루 함유된 건강한 식단입니다. 균형 잡힌 비타민과 식이섬유가 풍부하여 지친 학업에 큰 활력이 되어 줍니다.\n💡 [오늘의 음식 정보] 신선하고 정성스레 조리된 음식으로 소화가 잘 되고 두뇌 회전에도 큰 도움을 줍니다. 남김없이 맛있게 먹고 오늘 하루도 힘내세요!"
            }
            
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$geminiKey"
            
            val systemPrompt = "You are a professional nutritionist and friendly school dietician. Speak in warm and engaging Korean."
            val userPrompt = """
                다음 학교 급식 메뉴를 분석해서 영양소 분석, 예상 맛 후기, 그리고 급식에 얽힌 흥미롭고 재미있는 오늘의 음식 정보(재미있는 잡학지식이나 유래 등)를 3줄 내외로 아주 쉽고 친근하게 한글로 작성해줘.
                
                급식 메뉴: $mealDescription
                
                포맷 예시:
                🌱 [영양소&맛] 찹쌀밥과 갈비탕으로 철분과 에너지가 보충되는 든든하고 따끈한 한 끼예요! 겉절이의 아삭한 매콤함이 입맛을 상큼하게 돋워준답니다.
                💡 [오늘의 음식 퀴즈!] 아삭아삭한 '오이'는 토마토 다음으로 수분이 가득한 채소(95%)여서 공부하느라 지친 뇌를 촉촉하게 채워준다는 사실, 알고 계셨나요?
            """.trimIndent()

            val requestBodyJson = JSONObject()
            
            val systemInstructionJson = JSONObject()
            val systemParts = JSONObject().put("text", systemPrompt)
            systemInstructionJson.put("parts", JSONObject.wrap(listOf(systemParts)))
            requestBodyJson.put("systemInstruction", systemInstructionJson)

            val partJson = JSONObject().put("text", userPrompt)
            val contentJson = JSONObject().put("parts", JSONObject.wrap(listOf(partJson)))
            requestBodyJson.put("contents", JSONObject.wrap(listOf(contentJson)))

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val okRequestBody = requestBodyJson.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(url)
                .post(okRequestBody)
                .build()

            try {
                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: return@withGeminiKey "영양 분석 오류가 발생했습니다."
                    val resJson = JSONObject(responseBody)
                    
                    val candidates = resJson.getJSONArray("candidates")
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.getJSONObject("content")
                    val partsArray = contentObj.getJSONArray("parts")
                    val rawText = partsArray.getJSONObject(0).getString("text")
                    
                    return@withGeminiKey rawText.trim()
                }
            } catch (e: Exception) {
                Log.e("GEMINI_API", "Error calling Gemini API", e)
                return@withGeminiKey "급식 메뉴 분석에 실패했습니다. 네트워크 연결을 확인해주세요."
            }
        }
    }
}
