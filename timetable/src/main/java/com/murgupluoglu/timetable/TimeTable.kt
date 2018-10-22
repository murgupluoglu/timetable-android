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
class TimeTable @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    private val LOG_ENABLE = BuildConfig.DEBUG


    private val mPerTextCounts = intArrayOf(
        60, 60, 2 * 60, 4 * 60, // 10s/unit: 1min, 2min, 4min
        5 * 60, 10 * 60, // 1min/unit: 5min, 10min
        20 * 60, 30 * 60, // 5min/unit: 20min, 30min
        3600, 2 * 3600, 3 * 3600, 4 * 3600, 5 * 3600, 6 * 3600 // 15min/unit
    )

    private val minTimePartWidthMin = 24
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


    private var mUnitGap = 0f

    private var mUnitSecond = 60

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
        var startTime: Int = 0

        /**
         * End time must be greater than [.startTime]
         */
        var endTime: Int = 0

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
        val ta = context.obtainStyledAttributes(attrs, R.styleable.TimeTable)
        bgColor = ta.getColor(R.styleable.TimeTable_backgroundColor, Color.parseColor("#EEEEEE"))
        seperatorColor = ta.getColor(R.styleable.TimeTable_seperatorColor, Color.GRAY)
        partHeight = ta.getDimension(R.styleable.TimeTable_partHeight, dp2px(20f).toFloat())
        partColor = ta.getColor(R.styleable.TimeTable_partColor, Color.parseColor("#8F2CFA"))
        gradationWidth = ta.getDimension(R.styleable.TimeTable_seperatorWidth, 1f)
        seperatorTextColor = ta.getColor(R.styleable.TimeTable_seperatorTextColor, Color.GRAY)
        seperatorTextSize = ta.getDimension(R.styleable.TimeTable_seperatorTextSize, sp2px(12f).toFloat())
        seperatorTextGap = ta.getDimension(R.styleable.TimeTable_seperatorTextGap, dp2px(2f).toFloat())
        currentTime = ta.getInt(R.styleable.TimeTable_currentTime, 0)
        indicatorTriangleSideLen = ta.getDimension(R.styleable.TimeTable_indicatorTriangleSideLen, dp2px(15f).toFloat())
        indicatorWidth = ta.getDimension(R.styleable.TimeTable_indicatorLineWidth, dp2px(1f).toFloat())
        indicatorColor = ta.getColor(R.styleable.TimeTable_indicatorLineColor, Color.RED)
        ta.recycle()
    }

    private fun calculateValues() {
        mCurrentDistance = currentTime / mUnitSecond * mUnitGap
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
        mCurrentDistance = currentTime.toFloat() / mUnitSecond * mUnitGap
        invalidate()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mWidth = View.MeasureSpec.getSize(widthMeasureSpec)
        mHeight = View.MeasureSpec.getSize(heightMeasureSpec)

        mUnitSecond = 60
        mUnitGap = mWidth / 188f // we want to fit 3hour 8min inside phone width = 188min -> width/188

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

        logD("x %s, y %s ", x, y)

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker!!.addMovement(event)
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                logD("ACTION_DOWN")
                mTimePartList?.forEachIndexed { index, timePart ->
                    val start = calculateTimePartStart(timePart.startTime)
                    val end = calculateTimePartEnd(timePart.endTime)
                    if(x in (start + 1)..(end - 1)){
                        Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
                        isClickedAnyTimePart = true
                        clickedTimePartIndex = index
                    }
                }
                if(!isClickedAnyTimePart){
                    isMoving = false
                    mInitialX = x
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
                    logD("mCurrentDistance %s ", mCurrentDistance)
                }else{
                    val start = mTimePartList.get(clickedTimePartIndex).startTime
                    val end = mTimePartList.get(clickedTimePartIndex).endTime
                    mTimePartList.get(clickedTimePartIndex).startTime = start + 100
                    mTimePartList.get(clickedTimePartIndex).endTime = end + 100
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
                        val maxDistance = (MAX_TIME_VALUE / mUnitGap * mUnitGap).toInt()
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

    private fun computeTime() {
        var maxDistance = MAX_TIME_VALUE / mUnitSecond * mUnitGap
        maxDistance -= mWidth //For stop end of 23:00
        mCurrentDistance = Math.min(maxDistance, Math.max(0f, mCurrentDistance))
        currentTime = (mCurrentDistance / mUnitGap * mUnitSecond).toInt()
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

            start += mUnitSecond
            offset += mUnitGap
        }
    }

    private fun calculateTimePartStart(startTime : Int) :  Int{
        val secondGap = mUnitGap / mUnitSecond
        return (0 - mCurrentDistance + startTime * secondGap).toInt()
    }

    private fun calculateTimePartEnd(endTime : Int) : Int{
        val secondGap = mUnitGap / mUnitSecond
        return (0 - mCurrentDistance +endTime * secondGap).toInt()
    }


    private fun drawTimeParts(canvas: Canvas) {
        if (mTimePartList == null) {
            return
        }

        mPaint!!.strokeWidth = mHeight.toFloat()
        mPaint!!.color = partColor
        val yPosition = mHeight / 2
        var start: Int
        var end: Int


        mTimePartList!!.forEachIndexed { index, timePart ->

            //draw timepart background
            start = calculateTimePartStart(timePart.startTime)
            end = calculateTimePartEnd(timePart.endTime)
            canvas.drawLine(start.toFloat(), yPosition.toFloat(), end.toFloat(), yPosition.toFloat(), mPaint!!)

            //draw left top start time
            val textStart = formatTimeHHmm(timePart.startTime)
            canvas.drawText(textStart, (start + textStartMargin).toFloat(), partHeight, mTextPaint!!)

            //draw right bottom end time
            val textEnd = formatTimeHHmm(timePart.endTime)
            canvas.drawText(
                textEnd,
                end.toFloat() - mTextHalfWidth * 2 - textStartMargin.toFloat(),
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
            Log.d("MoneySelectRuleView", String.format("zjun@$format", *args))
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