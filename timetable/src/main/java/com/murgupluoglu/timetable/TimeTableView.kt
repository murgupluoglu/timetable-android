package com.murgupluoglu.timetable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.annotation.FloatRange
import android.support.annotation.IntRange
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.Scroller
import android.widget.Toast
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Mustafa Urgupluoglu
 * Date 22/10/2018
 */
class TimeTableView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        View(context, attrs, defStyleAttr) {

    private var isInitialized = false
    private var startClickTime = 0L
    var isLogEnabled = false

    private val textStartMargin = dp2px(2f)
    private val centerImageSize = dp2px(24f)
    private val textHalfWidth: Float


    @IntRange(from = 0, to = MAX_TIME_VALUE.toLong())
    private var currentTimeSecond: Int = 0

    private var backgroundcolor: Int = 0

    //Part
    private var partColor: Int = 0

    private var timePartMinFloat: Float = 24.minuteToFloat() //when you resize any time-part cannot be smaller than this number 24min

    //Handles
    lateinit var handlesPaint: Paint
    private var handlesWidth: Int = dp2px(4f)
    private var handlesTouchAreaFloat = 0f

    //Seperator
    private var seperatorColor: Int = 0
    private var seperatorWidth: Float = 0f
    private var seperatorTextColor: Int = 0
    private var seperatorTextSize: Float = 0f
    private var seperatorTextTopMargin: Float = 0f
    private var seperatorTextLeftMargin: Float = 0f

    private var indicatorColor: Int = 0
    private var indicatorWidth: Float = 0.toFloat()

    private var maxTimePixel: Float = 0f
    private var isScrolling = false
    private var isMoving = false
    private var currentPosition: Float = 0f //Distance between current time and 00:00

    lateinit var paint: Paint
    private var textPaint: TextPaint? = null
    lateinit var scroller: Scroller

    private var widthView: Int = 0
    private var heightView: Int = 0

    private var clickedTimePartIndex: Int = 0
    private var timePartList: LinkedList<TimePart> = LinkedList()

    var timeTableListener: TimeTableListener? = null

    interface TimeTableListener {
        fun initialized()
        fun onTimeChanged(newTimeValue: Int)
    }

    private var timePartClickedPart: TimePartParts = TimePartParts.NOT_CLICKED

    enum class TimePartParts {
        NOT_CLICKED,
        LEFT_HANDLE,
        CENTER,
        RIGHT_HANDLE
    }


    class TimePart {
        /**
         * view float, range of values [0, 24]
         * 0       —— 00:00
         * 24   —— 23:59
         */
        var start: Float = 0f

        /**
         * End time must be greater than [.start]
         */
        var end: Float = 0f

        /**
         * Center image 24x24
         */
        var centerImageName: String? = null

        override fun toString(): String {
            return "start ${start.floatToSecond()} \n end ${end.floatToSecond()} \n image $centerImageName"
        }
    }

    private val gestureDetector: GestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            logD("onDown ${e.x.toInt()}")

            val xPos = e.x.toInt()

            scroller.abortAnimation()

            timePartList.forEachIndexed { index, timePart ->

                val startPixel = (timePart.start - currentPosition).floatToPixel()
                val endPixel = (timePart.end - currentPosition).floatToPixel()

                val handleStartPixel = startPixel + handlesTouchAreaFloat.floatToPixel()
                val handleEndPixel = endPixel - handlesTouchAreaFloat.floatToPixel()

                when (xPos) {
                    in handleStartPixel..handleEndPixel -> {
                        timePartClickedPart = TimePartParts.CENTER
                        startClickTime = System.currentTimeMillis()
                        logD("Clicked Center")
                        //Toast.makeText(context, "Clicked Center", Toast.LENGTH_SHORT).show()
                    }
                    in (startPixel)..handleStartPixel -> {
                        timePartClickedPart = TimePartParts.LEFT_HANDLE
                        logD("Clicked LeftHandle")
                        Toast.makeText(context, "Clicked LeftHandle", Toast.LENGTH_SHORT).show()
                    }
                    in handleEndPixel..(endPixel) -> {
                        timePartClickedPart = TimePartParts.RIGHT_HANDLE
                        logD("Clicked RightHandle")
                        Toast.makeText(context, "Clicked RightHandle", Toast.LENGTH_SHORT).show()
                    }
                }

                if (xPos in (startPixel)..(endPixel)) {
                    clickedTimePartIndex = index
                    logD("clickedTimePartIndex $clickedTimePartIndex")
                }
            }


            return super.onDown(e)
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            //logD("onScroll timePartClickedPart:$timePartClickedPart")

            //Scroll view for dragging
            when (timePartClickedPart) {
                TimePartParts.CENTER -> {
                    val clickDuration = System.currentTimeMillis() - startClickTime > 100
                    logD("clickDuration $clickDuration isScrolling $isScrolling")
                    if (clickDuration && !isScrolling) {
                        moveTimePart(distanceX.pixelToFloat())
                    } else {
                        isScrolling = true
                        currentPosition += distanceX.pixelToFloat()
                    }
                }
                TimePartParts.LEFT_HANDLE, TimePartParts.RIGHT_HANDLE -> {
                    //Move time-part
                    moveTimePart(distanceX.pixelToFloat())
                }
                else -> {
                    isScrolling = true
                    currentPosition += distanceX.pixelToFloat()
                }
            }
            computeTime()

            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            logD("onFling isMoving:$isMoving")
            if (!isMoving) {
                isScrolling = true
                scroller.forceFinished(true)
                scroller.fling(currentPosition.floatToPixel().toInt(), 0, (-velocityX).toInt(), 0, 0, maxTimePixel.toInt(), 0, 0)
                computeTime()
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    })


    init {
        initAttrs(context, attrs)

        init(context)

        textHalfWidth = textPaint!!.measureText("00:00") * .5f

        currentPosition = currentTimeSecond.minuteToFloat()
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.TimeTableView)
        backgroundcolor = ta.getColor(R.styleable.TimeTableView_backgroundColor, Color.parseColor("#EEEEEE"))
        partColor = ta.getColor(R.styleable.TimeTableView_partColor, Color.parseColor("#8F2CFA"))
        seperatorColor = ta.getColor(R.styleable.TimeTableView_seperatorColor, Color.GRAY)
        seperatorWidth = ta.getDimension(R.styleable.TimeTableView_seperatorWidth, 1f)
        seperatorTextColor = ta.getColor(R.styleable.TimeTableView_seperatorTextColor, Color.GRAY)
        seperatorTextSize = ta.getDimension(R.styleable.TimeTableView_seperatorTextSize, sp2px(12f).toFloat())
        seperatorTextTopMargin = ta.getDimension(R.styleable.TimeTableView_seperatorTextTopMargin, dp2px(20f).toFloat())
        seperatorTextLeftMargin = ta.getDimension(R.styleable.TimeTableView_seperatorTextLeftMargin, dp2px(2f).toFloat())
        currentTimeSecond = ta.getInt(R.styleable.TimeTableView_currentTime, 0)
        indicatorWidth = ta.getDimension(R.styleable.TimeTableView_indicatorLineWidth, dp2px(1f).toFloat())
        indicatorColor = ta.getColor(R.styleable.TimeTableView_indicatorLineColor, Color.RED)
        ta.recycle()
    }

    private fun init(context: Context) {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = seperatorColor
        paint.strokeWidth = seperatorWidth

        handlesPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        handlesPaint.color = Color.WHITE
        handlesPaint.strokeWidth = handlesWidth.toFloat()

        textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint!!.textSize = seperatorTextSize
        textPaint!!.color = seperatorTextColor

        scroller = Scroller(context)

        currentPosition = currentTimeSecond.secondToFloat()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        widthView = View.MeasureSpec.getSize(widthMeasureSpec)
        heightView = View.MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(widthView, heightView)

        if (!isInitialized) {
            maxTimePixel = MAX_TIME_VALUE.secondToFloat().floatToPixel() - widthView //For stop end of 23:00
            handlesTouchAreaFloat = dp2px(10f).toFloat().pixelToFloat()

            isInitialized = true
            timeTableListener?.initialized()
        }
    }

//    private fun pixelToMin(pixel: Float): Int {
//        return (pixel / pixelPerMininute).toInt()
//    }
//
//    private fun minuteToPixel(min: Int): Float {
//        return min * pixelPerMininute
//    }

    //private var initX : Float = 0f
    private fun restStatusByAction(event: MotionEvent) {
        val action = event.action and MotionEvent.ACTION_MASK
        //val xPos = event.x
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                //initX = xPos
            }
            MotionEvent.ACTION_UP -> {
                logD("ACTION_UP")
                timePartClickedPart = TimePartParts.NOT_CLICKED
                isScrolling = false
                isMoving = false
            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        //logD("onTouchEvent $event")

        gestureDetector.onTouchEvent(event)
        restStatusByAction(event)
        return true
    }


    private fun moveTimePart(distanceXFloat: Float) {
        isMoving = true

        val timePart = timePartList[clickedTimePartIndex]
        val startPositionFloat = timePart.start - distanceXFloat
        val endPositionFloat = timePart.end - distanceXFloat

        logD("distanceXFloat $distanceXFloat diffStartFloat $startPositionFloat")

        var isPossible = true
        //check left and right time-parts if exist
        val prevIndex = clickedTimePartIndex - 1
        val nextIndex = clickedTimePartIndex + 1
        if(timePartClickedPart == TimePartParts.CENTER || timePartClickedPart == TimePartParts.LEFT_HANDLE){
            if(prevIndex >= 0){
                val prevTimePart = timePartList[prevIndex]
                if(prevTimePart.end > startPositionFloat){ //left side time-part end-value must be smaller than our start-value
                    isPossible = false
                }
            }
        }
        if(timePartClickedPart == TimePartParts.CENTER || timePartClickedPart == TimePartParts.RIGHT_HANDLE){
            if(nextIndex < timePartList.size){
                val nextTimePart = timePartList[nextIndex]
                if(endPositionFloat > nextTimePart.start){
                    isPossible = false
                }
            }
        }

        //logD("isPossible $isPossible")
        if(!isPossible) return
        when (timePartClickedPart) {
            TimePartParts.CENTER -> {
                if (startPositionFloat >= 0.0 && endPositionFloat < 24.0) {
                    timePartList[clickedTimePartIndex].start = startPositionFloat
                    timePartList[clickedTimePartIndex].end = endPositionFloat
                }
            }
            TimePartParts.LEFT_HANDLE -> {
                val minWidthDiff = timePart.end - startPositionFloat
                if (startPositionFloat >= 0.0 && minWidthDiff >= timePartMinFloat) {
                    timePartList[clickedTimePartIndex].start = startPositionFloat
                }
            }
            TimePartParts.RIGHT_HANDLE -> {
                val minWidthDiff = endPositionFloat - timePart.start
                if (endPositionFloat < 24.0 && minWidthDiff >= timePartMinFloat) {
                    timePartList[clickedTimePartIndex].end = endPositionFloat
                }
            }
            else -> {

            }
        }
    }

    private fun computeTime() {
        currentPosition = Math.min(24f, Math.max(0f, currentPosition))
        //logD("currentPositionPixel $currentPositionPixel maxTimePixel $maxTimePixel")
        currentTimeSecond = currentPosition.floatToSecond()
        if (timeTableListener != null) {
            timeTableListener!!.onTimeChanged(currentTimeSecond)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {

        canvas.drawColor(backgroundcolor)

        drawRule(canvas)

        drawTimeParts(canvas)
    }

    override fun computeScroll() {
        //logD("computeScroll")
        if (!isMoving && scroller.computeScrollOffset()) {
            currentPosition = scroller.currX.toFloat().pixelToFloat()
            computeTime()
        }
    }


    private fun drawRule(canvas: Canvas) {

        paint.color = seperatorColor
        paint.strokeWidth = seperatorWidth

        var floatSeconds: Float = 0f
        var offset: Float = (0 - currentPosition.floatToPixel())
        for(i in 0..23){

            canvas.drawLine(offset, 0f, offset, heightView.toFloat(), paint)

            val text = formatTimeHHmm(floatSeconds)
            canvas.drawText(text, offset + seperatorTextLeftMargin, seperatorTextTopMargin, textPaint!!)

            floatSeconds++
            offset += heightView
        }
    }

    private fun drawTimeParts(canvas: Canvas) {
        if (timePartList.isEmpty()) {
            return
        }

        paint.strokeWidth = heightView.toFloat()
        paint.color = partColor
        val yPosition : Float = (heightView / 2).toFloat()
        var start: Float
        var end: Float


        timePartList.forEachIndexed { index, timePart ->

            //draw timepart background
            start = (timePart.start - currentPosition).floatToPixel()
            end = (timePart.end - currentPosition).floatToPixel()
            canvas.drawLine(start, yPosition, end, yPosition, paint)


            //draw left top start time
            val textStart = formatTimeHHmm(timePart.start)
            canvas.drawText(textStart, (start + textStartMargin), seperatorTextTopMargin, textPaint!!)


            //draw right bottom end time
            val textEnd = formatTimeHHmm(timePart.end)
            canvas.drawText(
                    textEnd,
                    end - textHalfWidth * 2 - textStartMargin,
                    heightView - seperatorTextTopMargin / 2,
                    textPaint!!
            )


            //draw center image
            val resId = resources.getIdentifier(timePart.centerImageName, "drawable", context.packageName)
            val drawable = ContextCompat.getDrawable(context, resId)
            val xStart = start + (end - start) / 2 - centerImageSize / 2
            val yStart = heightView / 2 - centerImageSize / 2
            drawable!!.setBounds(xStart.toInt(), yStart, ((xStart + centerImageSize).toInt()), yStart + centerImageSize)
            drawable.draw(canvas)


            //draw left handle
            val xHandleLeft = (start + textStartMargin + 8f)
            val heightHandle = ((heightView / 2) - heightView / 8).toFloat()
            canvas.drawLine(xHandleLeft, heightHandle, xHandleLeft, heightHandle + heightView / 4, handlesPaint)


            //draw right handle
            val xHandleRight = (end - textStartMargin - 8f)
            canvas.drawLine(xHandleRight, heightHandle, xHandleRight, heightHandle + heightView / 4, handlesPaint)
        }
    }

    private fun dp2px(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
    }

    private fun sp2px(sp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics).toInt()
    }

    private fun logD(format: String, vararg args: Any) {
        if (isLogEnabled) {
            Log.e("TimeTableView", String.format(format, *args))
        }
    }

    fun addTimePart(timePart: TimePart) {
        var isPossible = true
        timePartList.forEach {
            if (timePart.start in it.start - 1..it.end - 1
                    || timePart.end in it.start - 1..it.end - 1) {
                isPossible = false
            }
        }

        if (isPossible) {
            timePartList.add(timePart)
            sortList()
        } else {
            Toast.makeText(context, "NOT_POSSIBLE", Toast.LENGTH_SHORT).show()
        }

        timePartList.forEachIndexed { index, part ->
            logD(" ***$index*** \n $part \n *****")
        }
        postInvalidate()
    }

    private fun sortList() {
        timePartList.sortWith(Comparator { p0, p1 ->
            (p0.start).compareTo(p1.end)
        })
    }


    fun setCurrentTimeMinute(@FloatRange(from = 0.0, to = 24.0) currentTimeFloat: Float) {
        this.currentTimeSecond = currentTimeFloat.floatToSecond()
        currentPosition = currentTimeFloat
        postInvalidate()
    }

    private fun formatTimeHHmm(@FloatRange(from = 0.0, to = 24.0) floatSecond: Float): String {

        val second = floatSecond.floatToSecond()
        val totalMinute = second / 60
        val hour = totalMinute / 60
        val minute = totalMinute % 60
        val sb = StringBuilder()
        if (hour < 10) {
            sb.append('0')
        }
        sb.append(hour).append(':')
        if (minute < 10) {
            sb.append('0')
        }
        sb.append(minute)
        return sb.toString()
    }

    private fun Float.floatToPixel(): Float {
        return heightView * this
    }

    private fun Float.pixelToFloat(): Float {
        return this / heightView
    }

    companion object {
        const val MAX_TIME_VALUE: Int = 24 * 60 * 60
    }

}

fun Int.secondToFloat(): Float {
    return this * 0.000277777777778f
}

fun Int.minuteToFloat(): Float {
    return TimeUnit.MINUTES.toSeconds(this.toLong()).toInt().secondToFloat()
}

fun Float.floatToSecond(): Int {
    return (this / 0.000277777777778f).toInt()
}