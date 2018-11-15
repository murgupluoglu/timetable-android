package com.murgupluoglu.timetablelib

import android.content.ClipData
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import com.murgupluoglu.timetable.TimeTableView
import com.murgupluoglu.timetable.minuteToFloat
import kotlinx.android.synthetic.main.activity_main.*
import android.util.Log
import android.view.*
import android.view.View.DRAG_FLAG_OPAQUE
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.flexbox.*
import kotlinx.android.synthetic.main.button_layout.view.*


class MainActivity : AppCompatActivity() {


    companion object {
        lateinit var shadowBuilder : View.DragShadowBuilder
    }
    val TAG = "MainActivity"

    val myDataset: ArrayList<RecyclerViewItem> = ArrayList()

    var shadowBottomMargin = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        shadowBottomMargin = dpTopixel(this@MainActivity, 80f)



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

            override fun onItemDeleted(position: Int, isHovered: Boolean, posX: Float) {
                Log.e("ON_ITEM_DELETED", "position $position isHovered $isHovered")
                val x = posX
                val y = timeTable.y / 2
                val iconName = myDataset.get(position).iconName
                Log.e("ON_ITEM_DELETED", "x:$x y:$y iconName:$iconName")
                moveShadow(x, y, iconName)

                val rootShadowLayout = createLayout(x, y, iconName) as RelativeLayout
                startDrag(rootShadowLayout.nameTextView, iconName)
                rootShadowLayout.visibility = View.GONE
            }
        }

        myDataset.addAll(getFakeData())


        val flexboxLayoutManager = FlexboxLayoutManager(this).apply {
            flexWrap = FlexWrap.WRAP
            flexDirection = FlexDirection.ROW
            alignItems = AlignItems.STRETCH
        }

        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = flexboxLayoutManager
            adapter = MyAdapter(myDataset, object : MyAdapterClickLister{
                override fun onClicked(position: Int, textView: TextView) {
                    Log.e(TAG, "LONG_CLICKED")

                    startDrag(textView, myDataset.get(position).iconName)
                    textView.visibility = View.INVISIBLE
                }
            })
        }


        rootConstraintLayout.setOnDragListener(MyDragListener())
        //timeTable.setOnDragListener(TimeTableDragListener())
    }

    fun createLayout(posX: Float, posY: Float, iconName: String): View {
        val inflater = LayoutInflater.from(this@MainActivity)

        val view = inflater.inflate(R.layout.button_layout, null)

        val textView = view.findViewById<TextView>(R.id.nameTextView)
        textView.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.rounded_background_white)

        val resId = resources.getIdentifier(iconName, "drawable", packageName)
        val drawable = ContextCompat.getDrawable(this@MainActivity, resId)!!
        textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

        view.x = posX
        view.y = posY

        view.id = R.id.rootShadowLayout

        rootConstraintLayout.addView(view)

        return view
    }

    fun startDrag(view : View, iconName: String){
        val clipData = ClipData.newPlainText(iconName, iconName)
        shadowBuilder = EmptyDragShadowBuilder(view)

        val flag = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) DRAG_FLAG_OPAQUE else 0
        ViewCompat.startDragAndDrop(view, clipData, shadowBuilder, view, flag)
    }

    fun moveShadow(posX : Float, posY : Float, iconName : String){

        var rootShadowLayout : RelativeLayout? = rootConstraintLayout.findViewById(R.id.rootShadowLayout)

        if(rootShadowLayout == null)
            rootShadowLayout = createLayout(posX, posY, iconName) as RelativeLayout

        rootShadowLayout.visibility = View.VISIBLE
        rootShadowLayout.x = posX - shadowBottomMargin/3
        rootShadowLayout.y = posY - shadowBottomMargin
    }

    fun dragEnded(){
        val rootShadowLayout : RelativeLayout? = rootConstraintLayout.findViewById(R.id.rootShadowLayout)
        if(rootShadowLayout != null){
            rootConstraintLayout.removeView(rootShadowLayout)
        }
    }

    fun dpTopixel(c: Context, dp: Float): Float {
        val density = c.resources.displayMetrics.density
        return dp * density
    }

    fun getFakeData() : ArrayList<RecyclerViewItem>{
        val array = ArrayList<RecyclerViewItem>()
        array.add(RecyclerViewItem("ic_cherry", "Cherry"))
        array.add(RecyclerViewItem("ic_peanut", "Peanut"))
        array.add(RecyclerViewItem("ic_onion", "Onion"))
        array.add(RecyclerViewItem("ic_tomato", "Tomato"))
        array.add(RecyclerViewItem("ic_pitaya", "Pitaya"))
        array.add(RecyclerViewItem("ic_durian", "Durian"))
        array.add(RecyclerViewItem("ic_lettuce", "Lettuce"))
        array.add(RecyclerViewItem("ic_broccoli", "Broccoli"))
        array.add(RecyclerViewItem("ic_corn", "Corn"))
        array.add(RecyclerViewItem("ic_breast_milk_fruit", "Breast Milk Fruit"))
        array.add(RecyclerViewItem("ic_blueberry", "Blueberry"))
        array.add(RecyclerViewItem("ic_potato", "Potato"))
        array.add(RecyclerViewItem("test", "Test"))
        return array
    }

    internal inner class MyDragListener : View.OnDragListener {

        override fun onDrag(v: View, event: DragEvent): Boolean {
            val returnedDraggedView = event.localState as TextView

            var iconName : String? = null
            if(event.clipDescription != null){
                iconName = event.clipDescription.label.toString()
            }

            if(iconName != null)
                moveShadow(event.x, event.y, iconName)

            val action = event.action
            when (action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    Log.e(TAG, "ACTION_DRAG_STARTED ${returnedDraggedView.text}")
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    Log.e(TAG, "ACTION_DRAG_LOCATION")
                }
                DragEvent.ACTION_DROP -> {
                    Log.e(TAG, "ACTION_DROP")
                    returnedDraggedView.visibility = View.VISIBLE

                    val rect = Rect()
                    timeTable.getHitRect(rect)
                    if(rect.contains(event.x.toInt(), event.y.toInt())) {
                        timeTable.addTimePartWithPosition(24.minuteToFloat(), event.x, iconName!!)
                    }
                    dragEnded()
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.e(TAG, "ACTION_DRAG_ENDED")
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    Log.e(TAG, "ACTION_DRAG_ENTERED")
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    Log.e(TAG, "ACTION_DRAG_EXITED")
                }
            }
            return true
        }
    }
}
