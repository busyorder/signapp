package com.busyorder.signapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.busyorder.signapp.R
import com.busyorder.signapp.model.Gesture
import java.text.SimpleDateFormat
import java.util.*

class GestureHistoryAdapter(
    private val gestureList: MutableList<Gesture>
) : RecyclerView.Adapter<GestureHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.txtGestureName)
        val confidence: TextView = view.findViewById(R.id.txtConfidence)
        val timestamp: TextView = view.findViewById(R.id.txtTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gesture_history, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = gestureList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val gesture = gestureList[position]

        // Gesture Name
        holder.name.text = gesture.label

        // Format Confidence (convert to %)
        val percent = (gesture.confidence * 100).toInt()
        holder.confidence.text = "Confidence: $percent%"

        // Format Timestamp
        val sdf = SimpleDateFormat("dd MMM yyyy  hh:mm a", Locale.getDefault())
        holder.timestamp.text = sdf.format(Date(gesture.timestamp))
    }

    // Optional helper for refresh
    fun updateData(newList: List<Gesture>) {
        gestureList.clear()
        gestureList.addAll(newList)
        notifyDataSetChanged()
    }
}