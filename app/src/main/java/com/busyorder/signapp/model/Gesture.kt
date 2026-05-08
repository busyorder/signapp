package com.busyorder.signapp.model

import java.text.SimpleDateFormat
import java.util.*

data class Gesture(
    val label: String,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
) {

    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy  hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getConfidencePercent(): Int {
        return (confidence * 100).toInt()
    }
}