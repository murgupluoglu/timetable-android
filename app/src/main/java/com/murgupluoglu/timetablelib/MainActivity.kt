package com.murgupluoglu.timetablelib

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.murgupluoglu.timetable.TimeTable
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = ArrayList<TimeTable.TimePart>()
        for (i in 0..0) {
            val part = TimeTable.TimePart()
            part.startTime = TimeUnit.MINUTES.toSeconds(36).toInt()
            part.endTime = part.startTime + TimeUnit.MINUTES.toSeconds(24).toInt()
            part.centerImageName = "ic_android_24dp"
            list.add(part)
        }
        timeTable.setTimePartList(list)
    }
}
