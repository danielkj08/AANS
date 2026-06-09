package com.example.util

object TimeExtractor {
    
    /**
     * Parses natural language strings to extract hours and minutes.
     * Returns Pair(hourOfDay, minute) or null if no time patterns are found.
     */
    fun extractTime(text: String): Pair<Int, Int>? {
        if (text.isEmpty()) return null

        // 1. Matches Korean relative prefixes: e.g. "오후 3시 20분", "오전 10시", "오후 4시 반", "오후 5시"
        val krAmPmRegex = """(오후|오전)\s*(\d{1,2})시(?:\s*(\d{1,2})분|\s*(반))?""".toRegex()
        val krAmPmMatch = krAmPmRegex.find(text)
        if (krAmPmMatch != null) {
            val amPm = krAmPmMatch.groupValues[1]
            var hour = krAmPmMatch.groupValues[2].toInt()
            val minStr = krAmPmMatch.groupValues[3]
            val isHalf = krAmPmMatch.groupValues[4] == "반"
            
            var minute = 0
            if (isHalf) {
                minute = 30
            } else if (minStr.isNotEmpty()) {
                minute = minStr.toIntOrNull() ?: 0
            }

            if (amPm == "오후" && hour < 12) {
                hour += 12
            } else if (amPm == "오전" && hour == 12) {
                hour = 0
            }
            return Pair(hour, minute)
        }

        // 2. Matches English AM/PM patterns: e.g. "3:30 PM", "10 AM", "4pm", "5:15 am"
        val enAmPmRegex = """(\d{1,2})(?::(\d{2}))?\s*(PM|AM|pm|am)""".toRegex()
        val enAmPmMatch = enAmPmRegex.find(text)
        if (enAmPmMatch != null) {
            var hour = enAmPmMatch.groupValues[1].toInt()
            val minStr = enAmPmMatch.groupValues[2]
            val amPm = enAmPmMatch.groupValues[3].uppercase()
            
            val minute = if (minStr.isNotEmpty()) minStr.toIntOrNull() ?: 0 else 0

            if (amPm == "PM" && hour < 12) {
                hour += 12
            } else if (amPm == "AM" && hour == 12) {
                hour = 0
            }
            return Pair(hour, minute)
        }

        // 3. Matches standard digital clocks: e.g. "15:30", "09:45"
        val digitalRegex = """(\d{1,2}):(\d{2})""".toRegex()
        val digitalMatch = digitalRegex.find(text)
        if (digitalMatch != null) {
            val hour = digitalMatch.groupValues[1].toInt()
            val minute = digitalMatch.groupValues[2].toInt()
            if (hour in 0..23 && minute in 0..59) {
                return Pair(hour, minute)
            }
        }

        // 4. Matches Korean short forms without prefixes: e.g. "3시 45분", "1시 반"
        val krSimpleRegex = """(\d{1,2})시(?:\s*(\d{1,2})분|\s*(반))?""".toRegex()
        val krSimpleMatch = krSimpleRegex.find(text)
        if (krSimpleMatch != null) {
            var hour = krSimpleMatch.groupValues[1].toInt()
            val minStr = krSimpleMatch.groupValues[2]
            val isHalf = krSimpleMatch.groupValues[3] == "반"
            
            var minute = 0
            if (isHalf) {
                minute = 30
            } else if (minStr.isNotEmpty()) {
                minute = minStr.toIntOrNull() ?: 0
            }

            // Intuitive School-Hour Adjustment:
            // In a school context, if hour is 1..7 (e.g. 3시, 4시), it almost certainly means PM (15:00, 16:00).
            if (hour in 1..7) {
                hour += 12
            }
            
            if (hour in 0..23 && minute in 0..59) {
                return Pair(hour, minute)
            }
        }

        return null
    }
}
