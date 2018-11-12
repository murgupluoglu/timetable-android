package com.murgupluoglu.timetablelib

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.murgupluoglu.timetable.TimeTableView
import com.murgupluoglu.timetable.minuteToFloat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeTable.isLogEnabled = BuildConfig.DEBUG
        timeTable.timeTableListener = object : TimeTableView.TimeTableListener{
            override fun initialized() {

                val part = TimeTableView.TimePart()
                part.start = 0.minuteToFloat()
                part.end = part.start + 24.minuteToFloat()
                part.centerImageName = "ic_android_24dp"

                timeTable.addTimePart(part)
                //timeTable.setCurrentTimeMinute(122.minuteToFloat())
            }

            override fun onTimeChanged(newTimeValue: Int) {

            }
        }

        buttonAddTimePart.setOnClickListener {
            val part = TimeTableView.TimePart()
            part.start = 60.minuteToFloat()
            part.end = part.start + 24.minuteToFloat()
            part.centerImageName = "ic_android_24dp"

            timeTable.addTimePart(part)
        }
    }
}
