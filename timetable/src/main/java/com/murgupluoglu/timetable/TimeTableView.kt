package com.murgupluoglu.timetable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.support.annotation.IntRange
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.Scroller
import android.widget.Toast

/**
 * Mustafa Urgupluoglu
 * Date 22/10/2018
 */
class TimeTableView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    private val LOG_ENABLE = BuildConfig.DEBUG

    private val textStartMargin = dp2px(2f)
    private val centerImageSize = dp2px(24f)

    private var bgColor: Int = 0
    private var seperatorColor: Int = 0

    private var partHeight: Float = 0f

    private var partColor: Int = 0

    private var gradationWidth: Float = 0.toFloat()


    private var seperatorTextColor: Int = 0
    private var seperatorTextSize: Float = 0f
    private var seperatorTextGap: Float = 0f


    @IntRange(from = 0, to = MAX_TIME_VALUE.toLong())
    private var currentTime: Int = 0

    private var indicatorColor: Int = 0

    private var indicatorTriangleSideLen: Float = 0.toFloat()

    private var indicatorWidth: Float = 0.toFloat()


    private var oneMinToPixel = 0f

    private var secondToMin = 60

    private val mTextHalfWidth: Float

    private val SCROLL_SLOP: Int
    private val MIN_VELOCITY: Int
    private val MAX_VELOCITY: Int

    /**
     * Distance between current time and 00:00
     */
    private var mCurrentDistance: Float = 0f


    private var mPaint: Paint? = null
    private var mTextPaint: TextPaint? = null
    private var mTrianglePath: Path? = null
    private var mScroller: Scroller? = null
    private var mVelocityTracker: VelocityTracker? = null

    private var mWidth: Int = 0
    private var mHeight: Int = 0

    private var mInitialX: Int = 0
    private var mInitialStartSec: Int = 0
    private var mInitialEndSec: Int = 0
    private var mLastX: Int = 0
    private var mLastY: Int = 0
    private var isMoving: Boolean = false
    private var isClickedAnyTimePart: Boolean = false
    private var clickedTimePartIndex: Int = 0

    private var mTimePartList: List<TimePart> = arrayListOf()
    private var mListener: OnTimeChangedListener? = null

    interface OnTimeChangedListener {
        fun onTimeChanged(newTimeValue: Int)
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
    }

    init {
        initAttrs(context, attrs)

        init(context)

        mTextHalfWidth = mTextPaint!!.measureText("00:00") * .5f
        val viewConfiguration = ViewConfiguration.get(context)
        SCROLL_SLOP = viewConfiguration.scaledTouchSlop
        MIN_VELOCITY = viewConfiguration.scaledMinimumFlingVelocity
        MAX_VELOCITY = viewConfiguration.scaledMaximumFlingVelocity

        calculateValues()
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.TimeTableView)
        bgColor = ta.getColor(R.styleable.TimeTableView_backgroundColor, Color.parseColor("#EEEEEE"))
        seperatorColor = ta.getColor(R.styleable.TimeTableView_seperatorColor, Color.GRAY)
        partHeight = ta.getDimension(R.styleable.TimeTableView_partHeight, dp2px(20f).toFloat())
        partColor = ta.getColor(R.styleable.TimeTableView_partColor, Color.parseColor("#8F2CFA"))
        gradationWidth = ta.getDimension(R.styleable.TimeTableView_seperatorWidth, 1f)
        seperatorTextColor = ta.getColor(R.styleable.TimeTableView_seperatorTextColor, Color.GRAY)
        seperatorTextSize = ta.getDimension(R.styleable.TimeTableView_seperatorTextSize, sp2px(12f).toFloat())
        seperatorTextGap = ta.getDimension(R.styleable.TimeTableView_seperatorTextGap, dp2px(2f).toFloat())
        currentTime = ta.getInt(R.styleable.TimeTableView_currentTime, 0)
        indicatorTriangleSideLen = ta.getDimension(R.styleable.TimeTableView_indicatorTriangleSideLen, dp2px(15f).toFloat())
        indicatorWidth = ta.getDimension(R.styleable.TimeTableView_indicatorLineWidth, dp2px(1f).toFloat())
        indicatorColor = ta.getColor(R.styleable.TimeTableView_indicatorLineColor, Color.RED)
        ta.recycle()
    }

    private fun calculateValues() {
        mCurrentDistance = currentTime / secondToMin * oneMinToPixel
    }

    private fun init(context: Context) {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        mTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint!!.textSize = seperatorTextSize
        mTextPaint!!.color = seperatorTextColor

        mTrianglePath = Path()

        mScroller = Scroller(context)

        setScale()
    }

    private fun setScale() {
        mCurrentDistance = currentTime.toFloat() / secondToMin * oneMinToPixel
        invalidate()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mWidth = View.MeasureSpec.getSize(widthMeasureSpec)
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec)

        secondToMin = 60
        oneMinToPixel = mWidth / 188f // we want to fit 3hour 8min inside phone width = 188min -> width/188

        if (View.MeasureSpec.getMode(heightMeasureSpec) == View.MeasureSpec.AT_MOST) {
            mHeight = dp2px(100f)
        }

        setMeasuredDimension(mWidth, mHeight)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val actionIndex = event.actionIndex
        val pointerId = event.getPointerId(actionIndex)
        val actionMasked = event.actionMasked
        val action = event.action
        val pointerCount = event.pointerCount
        logD(
            "onTouchEvent: actionIndex=%d, pointerId=%d, actionMasked=%d, action=%d, pointerCount=%d",
            actionIndex,
            pointerId,
            actionMasked,
            action,
            pointerCount
        )
        val x = event.x.toInt()
        val y = event.y.toInt()

        //logD("x %s, y %s ", x, y)

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker!!.addMovement(event)
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                logD("ACTION_DOWN")
                mInitialX = x

                mTimePartList.forEachIndexed { index, timePart ->
                    val start = calculateTimePartStart(timePart.startTimeSec)
                    val end = calculateTimePartEnd(timePart.endTimeSec)
                    if(x in (start + 1)..(end - 1)){
                        Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
                        isClickedAnyTimePart = true
                        clickedTimePartIndex = index
                        mInitialStartSec = timePart.startTimeSec
                        mInitialEndSec = timePart.endTimeSec
                    }
                }
                if(!isClickedAnyTimePart){
                    isMoving = false
                    if (!mScroller!!.isFinished) {
                        mScroller!!.forceFinished(true)
                    }
                }else{

                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> isMoving = false
            MotionEvent.ACTION_MOVE -> {
                logD("ACTION_MOVE")
                if(!isClickedAnyTimePart){
                    val dx = x - mLastX
                    if (!isMoving) {
                        val dy = y - mLastY
                        if (Math.abs(x - mInitialX) <= SCROLL_SLOP || Math.abs(dx) <= Math.abs(dy)) {
                            return true
                        }
                        isMoving = true
                    }
                    mCurrentDistance -= dx.toFloat()
                }else{
                    val dx = (x - mInitialX)
                    val currentTimeDiff = (dx / oneMinToPixel  * secondToMin).toInt()
                    logD("dx %s currentTimeDiff %s mInitialStartMin %s mInitialEndMin %s", dx, currentTimeDiff, mInitialStartSec, mInitialEndSec)
                    setDifferenceToTimePart(currentTimeDiff)
                }
                computeTime()
            }
            MotionEvent.ACTION_UP -> {
                logD("ACTION_UP")
                if(!isClickedAnyTimePart){
                    if (!isMoving) {
                        return true
                    }
                    mVelocityTracker!!.computeCurrentVelocity(1000, MAX_VELOCITY.toFloat())
                    val xVelocity = mVelocityTracker!!.xVelocity.toInt()
                    if (Math.abs(xVelocity) >= MIN_VELOCITY) {
                        val maxDistance = (MAX_TIME_VALUE / oneMinToPixel * oneMinToPixel).toInt()
                        mScroller!!.fling(mCurrentDistance.toInt(), 0, -xVelocity, 0, 0, maxDistance, 0, 0)
                        invalidate()
                    }
                }
                isClickedAnyTimePart = false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if(!isClickedAnyTimePart){
                    val restIndex = if (actionIndex == 0) 1 else 0
                    mInitialX = event.getX(restIndex).toInt()
                }
            }
            else -> {
            }
        }
        mLastX = x
        mLastY = y
        return true
    }

    private fun setDifferenceToTimePart(differenceSec : Int){
        val calculatedStartDiff = mInitialStartSec + differenceSec
        val calculatedEndDiff = mInitialEndSec + differenceSec
        if(Math.abs(differenceSec) > secondToMin
        && calculatedStartDiff > 0 &&  calculatedEndDiff < MAX_TIME_VALUE){ //checking only +1 or -1 min can change, not seconds
            mTimePartList[clickedTimePartIndex].startTimeSec = calculatedStartDiff
            mTimePartList[clickedTimePartIndex].endTimeSec = calculatedEndDiff
        }
    }

    private fun computeTime() {
        var maxDistance = MAX_TIME_VALUE / secondToMin * oneMinToPixel
        maxDistance -= mWidth //For stop end of 23:00
        mCurrentDistance = Math.min(maxDistance, Math.max(0f, mCurrentDistance))
        currentTime = (mCurrentDistance / oneMinToPixel * secondToMin).toInt()
        if (mListener != null) {
            mListener!!.onTimeChanged(currentTime)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {

        canvas.drawColor(bgColor)

        drawRule(canvas)

        drawTimeParts(canvas)
    }

    override fun computeScroll() {
        if (mScroller!!.computeScrollOffset()) {
            mCurrentDistance = mScroller!!.currX.toFloat()
            computeTime()
        }
    }


    private fun drawRule(canvas: Canvas) {

        mPaint!!.color = seperatorColor
        mPaint!!.strokeWidth = gradationWidth

        var start = 0
        var offset = 0 - mCurrentDistance
        while (start <= MAX_TIME_VALUE) {

            if (start % 3600 == 0) {
                canvas.drawLine(offset, 0f, offset, mHeight.toFloat(), mPaint!!)

                val text = formatTimeHHmm(start)
                canvas.drawText(text, offset + 15, partHeight, mTextPaint!!)

//                logD(
//                    "mCurrentDistance= %s, offset=%f, hourLen=%f",
//                    mCurrentDistance,
//                    offset,
//                    partHeight
//                )
            }

            start += secondToMin
            offset += oneMinToPixel
        }
    }

    private fun calculateTimePartStart(startTime : Int) :  Int{
        val secondGap = oneMinToPixel / secondToMin
        return (0 - mCurrentDistance + startTime * secondGap).toInt()
    }

    private fun calculateTimePartEnd(endTime : Int) : Int{
        val secondGap = oneMinToPixel / secondToMin
        return (0 - mCurrentDistance +endTime * secondGap).toInt()
    }


    private fun drawTimeParts(canvas: Canvas) {
        if (mTimePartList.isEmpty()) {
            return
        }

        mPaint!!.strokeWidth = mHeight.toFloat()
        mPaint!!.color = partColor
        val yPosition = mHeight / 2
        var start: Int
        var end: Int


        mTimePartList.forEachIndexed { index, timePart ->

            //draw timepart background
            start = calculateTimePartStart(timePart.startTimeSec)
            end = calculateTimePartEnd(timePart.endTimeSec)
            canvas.drawLine(start.toFloat(), yPosition.toFloat(), end.toFloat(), yPosition.toFloat(), mPaint!!)

            //draw left top start time
            val textStart = formatTimeHHmm(timePart.startTimeSec)
            canvas.drawText(textStart, (start + textStartMargin).toFloat(), partHeight, mTextPaint!!)

            //draw right bottom end time
            val textEnd = formatTimeHHmm(timePart.endTimeSec)
            canvas.drawText(
                textEnd,
                end - mTextHalfWidth * 2 - textStartMargin,
                mHeight - partHeight / 2,
                mTextPaint!!
            )


            //draw center image
            val resId = resources.getIdentifier(timePart.centerImageName, "drawable", context.packageName)
            val drawable = ContextCompat.getDrawable(context, resId)
            val xStart = start + (end - start) / 2 - centerImageSize / 2
            val yStart = mHeight / 2 - centerImageSize / 2
            drawable!!.setBounds(xStart, yStart, xStart + centerImageSize, yStart + centerImageSize)
            drawable.draw(canvas)
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


    fun setOnTimeChangedListener(listener: OnTimeChangedListener) {
        this.mListener = listener
    }


    fun setTimePartList(timePartList: List<TimePart>) {
        this.mTimePartList = timePartList
        postInvalidate()
    }


    fun setCurrentTime(@IntRange(from = 0, to = MAX_TIME_VALUE.toLong()) currentTime: Int) {
        this.currentTime = currentTime
        calculateValues()
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