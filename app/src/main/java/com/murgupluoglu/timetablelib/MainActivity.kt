package com.murgupluoglu.timetablelib

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.murgupluoglu.timetable.TimeTableView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeTable.timeTableListener = object : TimeTableView.TimeTableListener{
            override fun initialized() {

                val part = TimeTableView.TimePart()
                part.startTimeSec = TimeUnit.MINUTES.toSeconds(36).toInt()
                part.endTimeSec = part.startTimeSec + TimeUnit.MINUTES.toSeconds(24).toInt()
                part.centerImageName = "ic_android_24dp"

                timeTable.addTimePart(part)
                //timeTable.setCurrentTime(TimeUnit.HOURS.toSeconds(3).toInt())
            }

            override fun onTimeChanged(newTimeValue: Int) {

            }
        }

        buttonAddTimePart.setOnClickListener {
            val part = TimeTableView.TimePart()
            part.startTimeSec = TimeUnit.MINUTES.toSeconds(61).toInt()
            part.endTimeSec = part.startTimeSec + TimeUnit.MINUTES.toSeconds(24).toInt()
            part.centerImageName = "ic_android_24dp"

            timeTable.addTimePart(part)
        }
    }
}
