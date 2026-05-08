package com.busyorder.signapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.busyorder.signapp.adapter.GestureHistoryAdapter
import com.busyorder.signapp.model.Gesture
import com.busyorder.signapp.utils.GestureStorage
import java.text.SimpleDateFormat
import java.util.*

class GestureHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GestureHistoryAdapter
    private lateinit var selectDateBtn: Button
    private lateinit var emptyText: TextView

    private val gestureList = mutableListOf<Gesture>()
    private val selectedDate = Calendar.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gesture_history)

        recyclerView = findViewById(R.id.historyRecyclerView)
        selectDateBtn = findViewById(R.id.selectDateBtn)
        emptyText = findViewById(R.id.emptyText)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = GestureHistoryAdapter(gestureList)
        emptyText.visibility = View.VISIBLE
        recyclerView.adapter = adapter

        // Show today by default
        loadHistory(selectedDate)

        selectDateBtn.setOnClickListener {
            openDatePicker()
        }
    }

    private fun openDatePicker() {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, y, m, d ->
                selectedDate.set(y, m, d)
                loadHistory(selectedDate)
            },
            year,
            month,
            day
        )

        datePicker.show()
    }

    private fun loadHistory(date: Calendar) {
        val data = GestureStorage.getGesturesByDate(this, date)
        gestureList.clear()
        gestureList.addAll(GestureStorage.getGesturesByDate(this, date))
        adapter.notifyDataSetChanged()
        emptyText.visibility =
            if (data.isEmpty()) View.VISIBLE else View.GONE

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        selectDateBtn.text = sdf.format(date.time)
    }
}