package com.murgupluoglu.timetable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

/**
 * Mustafa Urgupluoglu
 * Date 22/10/2018
 */
class TimeTableView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        View(context, attrs, defStyleAttr) {

    private var isInitialized = false
    private val LOG_ENABLE = BuildConfig.DEBUG

    private val textStartMargin = dp2px(2f)
    private val centerImageSize = dp2px(24f)
    private val textHalfWidth: Float


    @IntRange(from = 0, to = MAX_TIME_VALUE.toLong())
    private var currentTimeSec: Int = 0

    private var backgroundcolor: Int = 0

    //Part
    private var partHeight: Float = 0f
    private var partColor: Int = 0

    private var timePartMinWidth: Int = 24 * 60 //when you resize any time-part cannot be smaller than this number 24min * 60sec = 1440sec

    //Handles
    lateinit var handlesPaint: Paint
    private var handlesWidth: Int = dp2px(4f)
    private var handlesTouchAreaWidth: Int = dp2px(10f)

    //Seperator
    private var seperatorColor: Int = 0
    private var seperatorWidth: Float = 0f
    private var seperatorTextColor: Int = 0
    private var seperatorTextSize: Float = 0f
    private var seperatorTextGap: Float = 0f

    private var indicatorColor: Int = 0
    private var indicatorWidth: Float = 0.toFloat()

    private var onePixelMin = 0f
    private var onePixelSec = 0f
    private var secondToMin = 60

    private var isScrolling = false
    private var isMoving = false
    private var currentPositionPixel: Float = 0f //Distance between current time and 00:00

    lateinit var paint: Paint
    private var textPaint: TextPaint? = null
    private var scroller: Scroller? = null

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
         * Second, range of values [0, 86399]
         * 0       —— 00:00:00
         * 86399   —— 23:59:59
         */
        var startTimeSec: Int = 0

        /**
         * End time must be greater than [.startTime]
         */
        var endTimeSec: Int = 0

        /**
         * Center image 24x24
         */
        var centerImageName: String? = null

        override fun toString(): String {
            return "start $startTimeSec \n end $endTimeSec \n image $centerImageName"
        }
    }

    private val gestureDetector: GestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            //logD("onScroll distanceX:$distanceX")8


            //Scroll view for dragging
            when (timePartClickedPart) {
                TimePartParts.CENTER -> {
                    if (!isScrolling) {
                        isMoving = true
                        moveTimePart(distanceX)
                    } else {
                        isScrolling = true
                        currentPositionPixel += distanceX
                    }
                }
                TimePartParts.LEFT_HANDLE, TimePartParts.RIGHT_HANDLE -> {
                    //Move time-part
                    isMoving = true
                    moveTimePart(distanceX)
                }
                else -> {
                    isScrolling = true
                    currentPositionPixel += distanceX
                }
            }

            computeTime()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            logD("onFling isMoving:$isMoving")
            if (!isMoving) {
                isScrolling = true
                scroller!!.fling(currentPositionPixel.toInt(), 0, (-velocityX).toInt(), 0, 0, MAX_TIME_VALUE, 0, 0)
                computeTime()
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    })


    init {
        initAttrs(context, attrs)

        init(context)

        textHalfWidth = textPaint!!.measureText("00:00") * .5f

        currentPositionPixel = secondToPixel(currentTimeSec)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.TimeTableView)
        backgroundcolor = ta.getColor(R.styleable.TimeTableView_backgroundColor, Color.parseColor("#EEEEEE"))
        partHeight = ta.getDimension(R.styleable.TimeTableView_partHeight, dp2px(20f).toFloat())
        partColor = ta.getColor(R.styleable.TimeTableView_partColor, Color.parseColor("#8F2CFA"))
        seperatorColor = ta.getColor(R.styleable.TimeTableView_seperatorColor, Color.GRAY)
        seperatorWidth = ta.getDimension(R.styleable.TimeTableView_seperatorWidth, 1f)
        seperatorTextColor = ta.getColor(R.styleable.TimeTableView_seperatorTextColor, Color.GRAY)
        seperatorTextSize = ta.getDimension(R.styleable.TimeTableView_seperatorTextSize, sp2px(12f).toFloat())
        seperatorTextGap = ta.getDimension(R.styleable.TimeTableView_seperatorTextGap, dp2px(2f).toFloat())
        currentTimeSec = ta.getInt(R.styleable.TimeTableView_currentTime, 0)
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

        currentPositionPixel = secondToPixel(currentTimeSec)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        widthView = View.MeasureSpec.getSize(widthMeasureSpec)
        heightView = View.MeasureSpec.getSize(heightMeasureSpec)

        secondToMin = 60
        onePixelMin = widthView / 188f // we want to fit 3hour 8min inside phone width = 188min -> width/188
        onePixelSec = onePixelMin / secondToMin

        if (View.MeasureSpec.getMode(heightMeasureSpec) == View.MeasureSpec.AT_MOST) {
            heightView = dp2px(100f)
        }

        setMeasuredDimension(widthView, heightView)

        if (!isInitialized) {
            isInitialized = true
            timeTableListener?.initialized()
        }
    }

    private fun pixelToSec(pixel: Float): Float {
        return pixel / onePixelSec
    }

    private fun secondToPixel(second: Int): Float {
        return second * onePixelSec
    }

    private fun restStatusByAction(event: MotionEvent) {
        val action = event.action and MotionEvent.ACTION_MASK
        val x = event.x.toInt()
        when (action) {
            MotionEvent.ACTION_DOWN -> {

                timePartList.forEachIndexed { index, timePart ->
                    val start = secondToPixel(timePart.startTimeSec) - currentPositionPixel
                    val end = secondToPixel(timePart.endTimeSec) - currentPositionPixel
                    when (x) {
                        in (start + handlesTouchAreaWidth)..(end - handlesTouchAreaWidth) -> {
                            timePartClickedPart = TimePartParts.CENTER
                            logD("Clicked Center")
                            Toast.makeText(context, "Clicked Center", Toast.LENGTH_SHORT).show()
                        }
                        in (start)..(start + handlesTouchAreaWidth) -> {
                            timePartClickedPart = TimePartParts.LEFT_HANDLE
                            logD("Clicked LeftHandle")
                            Toast.makeText(context, "Clicked LeftHandle", Toast.LENGTH_SHORT).show()
                        }
                        in (end - handlesTouchAreaWidth)..(end) -> {
                            timePartClickedPart = TimePartParts.RIGHT_HANDLE
                            logD("Clicked RightHandle")
                            Toast.makeText(context, "Clicked RightHandle", Toast.LENGTH_SHORT).show()
                        }
                    }
                    if (x in (start)..(end)) {
                        clickedTimePartIndex = index
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                logD("ACTION_UP")
                timePartClickedPart = TimePartParts.NOT_CLICKED
            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        isScrolling = false
        isMoving = false
        scroller!!.abortAnimation()

        restStatusByAction(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun moveTimePart(distanceX: Float) {
        val differenceSec = -pixelToSec(distanceX).toInt()
        val timePart = timePartList[clickedTimePartIndex]
        val diffStart = timePart.startTimeSec + differenceSec
        val diffEnd = timePart.endTimeSec + differenceSec
        if (true) { //checking only +1 or -1 min can change, not seconds Math.abs(differenceSec) > secondToMin
            when (timePartClickedPart) {
                TimePartParts.CENTER -> {
                    if (diffStart > 0 && diffEnd < MAX_TIME_VALUE) {
                        timePartList[clickedTimePartIndex].startTimeSec = diffStart
                        timePartList[clickedTimePartIndex].endTimeSec = diffEnd
                    }
                }
                TimePartParts.LEFT_HANDLE -> {
                    val minWidthDiff = timePartList[clickedTimePartIndex].endTimeSec - diffStart
                    if (diffStart > 0
                            && minWidthDiff >= timePartMinWidth) {
                        timePartList[clickedTimePartIndex].startTimeSec = diffStart
                    }
                }
                TimePartParts.RIGHT_HANDLE -> {
                    val minWidthDiff = diffEnd - timePartList[clickedTimePartIndex].startTimeSec
                    if (diffEnd < MAX_TIME_VALUE
                            && minWidthDiff >= timePartMinWidth) {
                        timePartList[clickedTimePartIndex].endTimeSec = diffEnd
                    }
                }
                else -> {

                }
            }
        }
    }

    private fun computeTime() {
        var maxDistancePixel = secondToPixel(MAX_TIME_VALUE)
        maxDistancePixel -= widthView //For stop end of 23:00
        currentPositionPixel = Math.min(maxDistancePixel, Math.max(0f, currentPositionPixel))
        currentTimeSec = pixelToSec(currentPositionPixel).toInt()
        if (timeTableListener != null) {
            timeTableListener!!.onTimeChanged(currentTimeSec)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {

        canvas.drawColor(backgroundcolor)

        drawRule(canvas)

        drawTimeParts(canvas)
    }

    override fun computeScroll() {
        logD("computeScroll")
        if (!isMoving && scroller!!.computeScrollOffset()) {
            currentPositionPixel = scroller!!.currX.toFloat()
            computeTime()
        }
    }


    private fun drawRule(canvas: Canvas) {

        paint.color = seperatorColor
        paint.strokeWidth = seperatorWidth

        var start = 0
        var offset = 0 - currentPositionPixel
        while (start <= MAX_TIME_VALUE) {

            if (start % 3600 == 0) { // 1 hour
                canvas.drawLine(offset, 0f, offset, heightView.toFloat(), paint)

                val text = formatTimeHHmm(start)
                canvas.drawText(text, offset + 15, partHeight, textPaint!!)
            }

            start += secondToMin
            offset += onePixelMin
        }
    }

    private fun drawTimeParts(canvas: Canvas) {
        if (timePartList.isEmpty()) {
            return
        }

        paint.strokeWidth = heightView.toFloat()
        paint.color = partColor
        val yPosition = heightView / 2
        var start: Float
        var end: Float


        timePartList.forEachIndexed { index, timePart ->

            logD("currentPositionPixel $currentPositionPixel")
            //draw timepart background
            start = secondToPixel(timePart.startTimeSec) - currentPositionPixel
            end = secondToPixel(timePart.endTimeSec) - currentPositionPixel
            canvas.drawLine(start, yPosition.toFloat(), end, yPosition.toFloat(), paint)

            //draw left top start time
            val textStart = formatTimeHHmm(timePart.startTimeSec)
            canvas.drawText(textStart, (start + textStartMargin), partHeight, textPaint!!)

            //draw right bottom end time
            val textEnd = formatTimeHHmm(timePart.endTimeSec)
            canvas.drawText(
                    textEnd,
                    end - textHalfWidth * 2 - textStartMargin,
                    heightView - partHeight / 2,
                    textPaint!!
            )


            //draw center image
            val resId = resources.getIdentifier(timePart.centerImageName, "drawable", context.packageName)
            val drawable = ContextCompat.getDrawable(context, resId)
            val xStart = start + (end - start) / 2 - centerImageSize / 2
            val yStart = heightView / 2 - centerImageSize / 2
            drawable!!.setBounds(xStart.toInt(), yStart, (xStart + centerImageSize).toInt(), yStart + centerImageSize)
            drawable.draw(canvas)

            //draw left handle
            val xHandleLeft = start + textStartMargin
            val heightHandle = ((heightView / 2) - heightView / 8).toFloat()
            canvas.drawLine(xHandleLeft, heightHandle, xHandleLeft, heightHandle + heightView / 4, handlesPaint)

            //draw right handle
            val xHandleRight = end - textStartMargin
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
        if (LOG_ENABLE) {
            Log.e("TimeTableView", String.format(format, *args))
        }
    }

    fun addTimePart(timePart: TimePart) {
        var isPossible = true
        timePartList.forEach {
            if (timePart.startTimeSec in it.startTimeSec..it.endTimeSec
                    || timePart.endTimeSec in it.startTimeSec..it.endTimeSec) {
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
            Integer.valueOf(p0.startTimeSec).compareTo(p1.startTimeSec)
        })
    }


    fun setCurrentTime(@IntRange(from = 0, to = MAX_TIME_VALUE.toLong()) currentTime: Int) {
        this.currentTimeSec = currentTime
        currentPositionPixel = secondToPixel(currentTimeSec)
        postInvalidate()
    }

    fun formatTimeHHmm(@IntRange(from = 0, to = MAX_TIME_VALUE.toLong()) tValue: Int): String {
        var timeValue = tValue
        if (timeValue < 0) {
            timeValue = 0
        }
        val hour = timeValue / 3600
        val minute = timeValue % 3600 / 60
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


    fun formatTimeHHmmss(@IntRange(from = 0, to = MAX_TIME_VALUE.toLong()) timeValue: Int): String {
        val hour = timeValue / 3600
        val minute = timeValue % 3600 / 60
        val second = timeValue % 3600 % 60
        val sb = StringBuilder()

        if (hour < 10) {
            sb.append('0')
        }
        sb.append(hour).append(':')

        if (minute < 10) {
            sb.append('0')
        }
        sb.append(minute)
        sb.append(':')

        if (second < 10) {
            sb.append('0')
        }
        sb.append(second)
        return sb.toString()
    }

    companion object {
        const val MAX_TIME_VALUE = 24 * 3600
    }

}