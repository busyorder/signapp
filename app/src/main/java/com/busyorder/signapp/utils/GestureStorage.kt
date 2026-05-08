package com.busyorder.signapp.utils

import android.content.Context
import com.busyorder.signapp.model.Gesture
import java.util.*

object GestureStorage {

    private const val PREF_NAME = "gesture_history"
    private const val KEY_HISTORY = "history"

    fun saveGesture(context: Context, label: String, confidence: Float) {

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_HISTORY, "") ?: ""

        val newEntry = "$label|$confidence|${System.currentTimeMillis()};"

        prefs.edit()
            .putString(KEY_HISTORY, existing + newEntry)
            .apply()
    }

    fun getGesturesByDate(context: Context, selectedDate: Calendar): List<Gesture> {

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val historyString = prefs.getString(KEY_HISTORY, "") ?: ""

        val startCal = selectedDate.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = selectedDate.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)

        val start = startCal.timeInMillis
        val end = endCal.timeInMillis

        val gestureList = mutableListOf<Gesture>()
        val entries = historyString.split(";")

        for (entry in entries) {

            if (entry.isNotEmpty()) {

                val parts = entry.split("|")

                if (parts.size == 3) {

                    val label = parts[0]
                    val confidence = parts[1].toFloatOrNull() ?: 0f
                    val timestamp = parts[2].toLongOrNull() ?: 0L

                    if (timestamp in start..end) {
                        gestureList.add(Gesture(label, confidence, timestamp))
                    }
                }
            }
        }

        return gestureList
    }
}