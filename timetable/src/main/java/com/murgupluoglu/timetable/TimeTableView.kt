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
    private var currentTimeMin: Int = 0

    private var backgroundcolor: Int = 0

    //Part
    private var partHeight: Float = 0f
    private var partColor: Int = 0

    private var timePartMinWidth: Int = 24 //when you resize any time-part cannot be smaller than this number 24min

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

    private var pixelPerMininute : Int = 0

    private var maxTimePixel = 0
    private var isScrolling = false
    private var isMoving = false
    private var currentPositionPixel: Int = 0 //Distance between current time and 00:00

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
         * Second, range of values [0, 1439]
         * 0       —— 00:00
         * 1439   —— 23:59
         */
        var startTimeMin: Int = 0

        /**
         * End time must be greater than [.startTime]
         */
        var endTimeMin: Int = 0

        /**
         * Center image 24x24
         */
        var centerImageName: String? = null

        override fun toString(): String {
            return "start $startTimeMin \n end $endTimeMin \n image $centerImageName"
        }
    }

    private val gestureDetector: GestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            logD("onScroll timePartClickedPart:$timePartClickedPart diff ${(e2.x - e1.x).toInt()}")


            //Scroll view for dragging
            if(timePartClickedPart != TimePartParts.NOT_CLICKED){
                moveTimePart((distanceX.toInt()))
            }
//            when (timePartClickedPart) {
//                TimePartParts.CENTER -> {
//                    if (!isScrolling) {
//                        isMoving = true
//                        moveTimePart((distanceX.toInt()))
//                    } else {
//                        isScrolling = true
//                        currentPositionPixel += distanceX.toInt()
//                    }
//                }
//                TimePartParts.LEFT_HANDLE, TimePartParts.RIGHT_HANDLE -> {
//                    //Move time-part
//                    isMoving = true
//                    moveTimePart((distanceX.toInt()))
//                }
//                else -> {
//                    isScrolling = true
//                    currentPositionPixel += distanceX.toInt()
//                }
//            }

            computeTime()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            logD("onFling isMoving:$isMoving")
            if (!isMoving) {
                isScrolling = true
                scroller.forceFinished(true)
                scroller.fling(currentPositionPixel, 0, (-velocityX).toInt(), 0, 0, maxTimePixel, 0, 0)
                computeTime()
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    })


    init {
        initAttrs(context, attrs)

        init(context)

        textHalfWidth = textPaint!!.measureText("00:00") * .5f

        currentPositionPixel = minuteToPixel(currentTimeMin)
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
        currentTimeMin = ta.getInt(R.styleable.TimeTableView_currentTime, 0)
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

        currentPositionPixel = minuteToPixel(currentTimeMin)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        widthView = View.MeasureSpec.getSize(widthMeasureSpec)
        heightView = View.MeasureSpec.getSize(heightMeasureSpec)

        pixelPerMininute = (widthView / 188f).toInt() // we want to fit 3hour 8min inside phone width = 188min -> width/188
        maxTimePixel = minuteToPixel(MAX_TIME_VALUE) - widthView //For stop end of 23:00

        if (View.MeasureSpec.getMode(heightMeasureSpec) == View.MeasureSpec.AT_MOST) {
            heightView = dp2px(100f)
        }

        setMeasuredDimension(widthView, heightView)

        if (!isInitialized) {
            isInitialized = true
            timeTableListener?.initialized()
        }
    }

    private fun pixelToMin(pixel: Int): Int {
        return pixel / pixelPerMininute
    }

    private fun minuteToPixel(min: Int): Int {
        return min * pixelPerMininute
    }

    private fun restStatusByAction(event: MotionEvent) {
        val action = event.action and MotionEvent.ACTION_MASK
        val x = event.x.toInt()
        when (action) {
            MotionEvent.ACTION_DOWN -> {

                timePartList.forEachIndexed { index, timePart ->
                    val start = minuteToPixel(timePart.startTimeMin) - currentPositionPixel
                    val end = minuteToPixel(timePart.endTimeMin) - currentPositionPixel
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
                isScrolling = false
                isMoving = false
                sortList()
                invalidate()
            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        logD("onTouchEvent")

        //scroller.abortAnimation()

        gestureDetector.onTouchEvent(event)
        restStatusByAction(event)
        return true
    }

    private fun moveTimePart(distanceX: Int) {
        isMoving = true
        var value = 1
        if(distanceX < 0){
            value = -1
        }
        val differenceMin = pixelToMin(distanceX)
        if(Math.abs(differenceMin) > 1){
            value = differenceMin
        }
        val timePart = timePartList[clickedTimePartIndex]
        val diffStart = timePart.startTimeMin - value
        val diffEnd = timePart.endTimeMin - value

        logD("DIFF_START____ $diffStart value: $value")

        var isPossible = true
        //check left and right time-parts if exist
        val prevIndex = clickedTimePartIndex - 1
        val nextIndex = clickedTimePartIndex + 1
        if(timePartClickedPart == TimePartParts.CENTER || timePartClickedPart == TimePartParts.LEFT_HANDLE){
            if(prevIndex >= 0){
                val prevTimePart = timePartList[prevIndex]
                if(prevTimePart.endTimeMin > diffStart){//left side time-part end-value must be smaller than our start-value
                    isPossible = false
                }
            }
        }
        if(timePartClickedPart == TimePartParts.CENTER || timePartClickedPart == TimePartParts.RIGHT_HANDLE){
            if(nextIndex < timePartList.size){
                val nextTimePart = timePartList[nextIndex]
                if(diffEnd > nextTimePart.startTimeMin){
                    isPossible = false
                }
            }
        }

        logD("isPossible $isPossible")
        if(!isPossible) return
        when (timePartClickedPart) {
            TimePartParts.CENTER -> {
                if (diffStart >= 0 && diffEnd < MAX_TIME_VALUE) {
                    timePartList[clickedTimePartIndex].startTimeMin = diffStart
                    timePartList[clickedTimePartIndex].endTimeMin = diffEnd
                }
            }
            TimePartParts.LEFT_HANDLE -> {
                val minWidthDiff = timePartList[clickedTimePartIndex].endTimeMin - diffStart
                if (diffStart >= 0
                        && minWidthDiff >= timePartMinWidth) {
                    timePartList[clickedTimePartIndex].startTimeMin = diffStart
                }
            }
            TimePartParts.RIGHT_HANDLE -> {
                val minWidthDiff = diffEnd - timePartList[clickedTimePartIndex].startTimeMin
                if (diffEnd < MAX_TIME_VALUE
                        && minWidthDiff >= timePartMinWidth) {
                    timePartList[clickedTimePartIndex].endTimeMin = diffEnd
                }
            }
            else -> {

            }
        }
    }

    private fun computeTime() {
        currentPositionPixel = Math.min(maxTimePixel, Math.max(0, currentPositionPixel))
        //logD("currentPositionPixel $currentPositionPixel maxTimePixel $maxTimePixel")
        currentTimeMin = pixelToMin(currentPositionPixel)
        if (timeTableListener != null) {
            timeTableListener!!.onTimeChanged(currentTimeMin)
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
            currentPositionPixel = scroller.currX
            computeTime()
        }
    }


    private fun drawRule(canvas: Canvas) {

        paint.color = seperatorColor
        paint.strokeWidth = seperatorWidth

        var start = 0
        var offset = (0 - currentPositionPixel).toFloat()
        while (start <= MAX_TIME_VALUE) {

            if (start % 60 == 0) { // 1 hour
                canvas.drawLine(offset, 0f, offset, heightView.toFloat(), paint)

                val text = formatTimeHHmm(start)
                canvas.drawText(text, offset + 15, partHeight, textPaint!!)
            }

            start += 1
            offset += pixelPerMininute
        }
    }

    private fun drawTimeParts(canvas: Canvas) {
        if (timePartList.isEmpty()) {
            return
        }

        paint.strokeWidth = heightView.toFloat()
        paint.color = partColor
        val yPosition = heightView / 2
        var start: Int
        var end: Int


        timePartList.forEachIndexed { index, timePart ->

            //draw timepart background
            start = minuteToPixel(timePart.startTimeMin) - currentPositionPixel
            logD("START__ ${minuteToPixel(timePart.startTimeMin)} currentPositionPixel $currentPositionPixel")
            end = minuteToPixel(timePart.endTimeMin) - currentPositionPixel
            canvas.drawLine((start - (pixelPerMininute /2)).toFloat(), yPosition.toFloat(), (end + (pixelPerMininute / 2)).toFloat(), yPosition.toFloat(), paint)

            //draw left top start time
            val textStart = formatTimeHHmm(timePart.startTimeMin)
            canvas.drawText(textStart, ((start + textStartMargin).toFloat()), partHeight, textPaint!!)

            //draw right bottom end time
            val textEnd = formatTimeHHmm(timePart.endTimeMin)
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
        if (LOG_ENABLE) {
            Log.e("TimeTableView", String.format(format, *args))
        }
    }

    fun addTimePart(timePart: TimePart) {
        var isPossible = true
        timePartList.forEach {
            if (timePart.startTimeMin in it.startTimeMin-1..it.endTimeMin-1
                    || timePart.endTimeMin in it.startTimeMin-1..it.endTimeMin-1) {
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
            Integer.valueOf(p0.startTimeMin).compareTo(p1.startTimeMin)
        })
    }


    fun setCurrentTime(@IntRange(from = 0, to = MAX_TIME_VALUE.toLong()) currentTime: Int) {
        this.currentTimeMin = currentTime
        currentPositionPixel = minuteToPixel(currentTimeMin)
        postInvalidate()
    }

    private fun formatTimeHHmm(@IntRange(from = 0, to = MAX_TIME_VALUE.toLong()) totalMinute: Int): String {

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

    companion object {
        const val MAX_TIME_VALUE = 24 * 60
    }

}