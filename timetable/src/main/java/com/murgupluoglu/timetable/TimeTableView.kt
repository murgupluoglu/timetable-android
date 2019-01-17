package com.murgupluoglu.timetable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.Scroller
import android.widget.Toast
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.core.content.res.ResourcesCompat
import java.util.*
import java.util.concurrent.TimeUnit
import android.graphics.Bitmap
import java.text.SimpleDateFormat


/**
 * Mustafa Urgupluoglu
 * Date 22/10/2018
 */
class TimeTableView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        View(context, attrs, defStyleAttr) {

    fun getEmptyBitmap() : Bitmap {
        val conf = Bitmap.Config.ARGB_8888
        return Bitmap.createBitmap(centerImageSize, centerImageSize, conf)
    }

    private var isInitialized = false
    var isLogEnabled = false

    private val textStartMargin = dp2px(2f)
    private val centerImageSize = dp2px(24f)


    @IntRange(from = 0, to = MAX_TIME_VALUE.toLong())
    private var currentTimeSecond: Int = 0

    private var ColorBackground: Int = 0

    //Part
    private var partColor: Int = 0

    /**
     *  when you resize any time-part cannot be smaller than this number
     */
    var timePartMinFloat: Float = 24.minuteToFloat()

    //Handles
    lateinit var handlesPaint: Paint
    private var handlesWidth: Int = dp2px(4f)
    private var handlesTouchAreaFloat = 0f

    //Seperator
    private var seperatorColor: Int = 0
    private var seperatorWidth: Float = 0f
    private var seperatorTextColor: Int = 0
    private var seperatorTextSize: Float = 0f
    private var seperatorTextFont: Typeface = Typeface.DEFAULT
    private var seperatorTextFontId = 0
    private var seperatorTextTopMargin: Float = 0f
    private var seperatorTextLeftMargin: Float = 0f

    private var indicatorColor: Int = 0
    private var indicatorWidth: Float = 0.toFloat()

    /**
     * Distance between current time and 00:00
     */
    private var currentPosition: Float = 0f
    private var maxTimePixel: Float = 0f

    private lateinit var paint: Paint
    private var textPaint: TextPaint? = null
    private var textPaintMaxWidth: Float = 0f
    lateinit var scroller: Scroller

    private var widthView: Int = 0
    private var heightView: Int = 0

    private var clickedTimePartIndex: Int = 0
    private var timePartList: LinkedList<TimePart> = LinkedList()

    var timeTableListener: TimeTableListener? = null

    interface TimeTableListener {
        fun initialized()
        fun onTimeChanged(newTimeValue: Int)
        fun onItemDeleted(timePart: TimePart, position: Int, isHovered: Boolean, posX: Float)
        fun getItemBitmap(timePart: TimePart, position: Int) : Bitmap
    }

    private var timePartClickedPart: TimePartParts = TimePartParts.NOT_CLICKED

    enum class TimePartParts {
        NOT_CLICKED,
        LEFT_HANDLE,
        CENTER,
        RIGHT_HANDLE
    }

    enum class MoveActions {
        DEFAULT,
        MOVING,
        SCROLLING,
        HOVERED
    }

    var currentMoveAction = MoveActions.DEFAULT


    class TimePart {
        /**
         * view float, range of values [0, TIME_END_MAX]
         * 0       —— 00:00
         * TIME_END_MAX   —— 23:59
         */
        var start: Float = 0f

        /**
         * End time must be greater than [.start]
         */
        var end: Float = 0f

        /**
         * Center image 24x24
         */
        var centerImage: String? = null

        override fun toString(): String {
            return "start ${start.floatToSecond()} \n" +
                    "end ${end.floatToSecond()} \n" +
                    "image $centerImage"
        }
    }

    private val gestureDetector: GestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {

        override fun onDown(event: MotionEvent): Boolean {
            logD("onDown ${event.x.toInt()}")
            waitScroll = System.currentTimeMillis()
            scroller.abortAnimation()

            timePartClickedPart = TimePartParts.NOT_CLICKED
            currentMoveAction = MoveActions.DEFAULT
            lastX = 0f
            firstTouchX = event.x
            firstTouchY = event.y


            val xPosFloat = event.x.pixelToFloat()


            timePartList.forEachIndexed { index, timePart ->

                val startPixel = (timePart.start - currentPosition)
                val endPixel = (timePart.end - currentPosition)

                val handleStartPixel = startPixel + handlesTouchAreaFloat
                val handleEndPixel = endPixel - handlesTouchAreaFloat

                when (xPosFloat) {
                    in handleStartPixel..handleEndPixel -> {
                        timePartClickedPart = TimePartParts.CENTER
                        //logD("Clicked Center")
                        //Toast.makeText(context, "Clicked Center", Toast.LENGTH_SHORT).show()
                    }
                    in (startPixel)..handleStartPixel -> {
                        timePartClickedPart = TimePartParts.LEFT_HANDLE
                        //logD("Clicked LeftHandle")
                        //Toast.makeText(context, "Clicked LeftHandle", Toast.LENGTH_SHORT).show()
                    }
                    in handleEndPixel..(endPixel) -> {
                        timePartClickedPart = TimePartParts.RIGHT_HANDLE
                        //logD("Clicked RightHandle")
                        //Toast.makeText(context, "Clicked RightHandle", Toast.LENGTH_SHORT).show()
                    }
                }

                if (xPosFloat in (startPixel)..(endPixel)) {
                    clickedTimePartIndex = index
                    //logD("clickedTimePartIndex $clickedTimePartIndex")
                }
            }

            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            //logD("onScroll timePartClickedPart:$timePartClickedPart currentMoveAction:$currentMoveAction")

            if (currentMoveAction != MoveActions.MOVING) {
                currentMoveAction = MoveActions.SCROLLING
                currentPosition += distanceX.pixelToFloat()
            }

            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            //logD("onFling currentMoveAction:$currentMoveAction")
            if (currentMoveAction != MoveActions.MOVING) {
                currentMoveAction = MoveActions.SCROLLING
                scroller.forceFinished(true)
                scroller.fling(
                        currentPosition.floatToPixel().toInt(),
                        0,
                        (-velocityX).toInt(),
                        0,
                        0,
                        maxTimePixel.toInt(),
                        0,
                        0
                )
            }
            return true
        }
    })

    init {
        initAttrs(context, attrs)

        init(context)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.TimeTableView)
        ColorBackground = ta.getColor(R.styleable.TimeTableView_backgroundColor, Color.parseColor("#EEEEEE"))
        partColor = ta.getColor(R.styleable.TimeTableView_partColor, Color.parseColor("#8F2CFA"))
        seperatorColor = ta.getColor(R.styleable.TimeTableView_seperatorColor, Color.GRAY)
        seperatorWidth = ta.getDimension(R.styleable.TimeTableView_seperatorWidth, 1f)
        seperatorTextColor = ta.getColor(R.styleable.TimeTableView_seperatorTextColor, Color.GRAY)
        seperatorTextSize = ta.getDimension(R.styleable.TimeTableView_seperatorTextSize, sp2px(12f).toFloat())
        seperatorTextTopMargin = ta.getDimension(R.styleable.TimeTableView_seperatorTextTopMargin, dp2px(20f).toFloat())
        seperatorTextLeftMargin = ta.getDimension(R.styleable.TimeTableView_seperatorTextLeftMargin, dp2px(2f).toFloat())
        currentTimeSecond = stringTimeToSecond(ta.getString(R.styleable.TimeTableView_currentTime) ?: "00:00")
        indicatorWidth = ta.getDimension(R.styleable.TimeTableView_indicatorLineWidth, dp2px(1f).toFloat())
        indicatorColor = ta.getColor(R.styleable.TimeTableView_indicatorLineColor, Color.RED)
        seperatorTextFontId = ta.getResourceId(R.styleable.TimeTableView_seperatorTextFontName, 0)

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
        if (seperatorTextFontId != 0 && !BuildConfig.DEBUG) {
            seperatorTextFont = ResourcesCompat.getFont(context, seperatorTextFontId)!!
            textPaint!!.typeface = seperatorTextFont
        }
        textPaintMaxWidth = textPaint!!.measureText("00:00")

        scroller = Scroller(context)

        currentPosition = currentTimeSecond.secondToFloat()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        widthView = View.MeasureSpec.getSize(widthMeasureSpec)
        heightView = View.MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(widthView, heightView)

        if (!isInitialized) {
            maxTimePixel = MAX_TIME_VALUE.secondToFloat().floatToPixel() - widthView
            handlesTouchAreaFloat = dp2px(10f).toFloat().pixelToFloat()

            isInitialized = true
            timeTableListener?.initialized()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        //logD("onTouchEvent $event")

        gestureDetector.onTouchEvent(event).apply {
            restStatusByAction(event)
        }

        computeTime()
        return true
    }

    var waitScroll = 0L //Wait for onScroll determinate or not
    var lastX = 0f
    var firstTouchX = 0f
    var firstTouchY = 0f
    private fun restStatusByAction(event: MotionEvent) {
        val action = event.action and MotionEvent.ACTION_MASK

        when (action) {
            MotionEvent.ACTION_MOVE -> {
                val diffX = lastX - event.x
                //val diffXAbs = Math.abs(diffX)
                val diffWithFirstX = Math.abs(firstTouchX - event.x)
                val diffWithFirstY = Math.abs(firstTouchY - event.y)
                //logD("diffWithFirst $diffWithFirstX")

                if (lastX != 0f && System.currentTimeMillis() - waitScroll > 100) {
                    //logD("ACTION_MOVE currentMoveAction $currentMoveAction diffXAbs $diffXAbs")
                    val distanceXFloat = diffX.pixelToFloat()

                    when (timePartClickedPart) {
                        TimePartParts.CENTER,
                        TimePartParts.LEFT_HANDLE,
                        TimePartParts.RIGHT_HANDLE -> {

                            if (currentMoveAction != MoveActions.SCROLLING && currentMoveAction != MoveActions.HOVERED) {

                                if (currentMoveAction == MoveActions.MOVING || diffWithFirstX > 15) {
                                    moveTimePart(distanceXFloat)
                                } else if (diffWithFirstY > 15) {
                                    //logD("HOVERED")
                                    currentMoveAction = MoveActions.HOVERED
                                    timeTableListener?.apply {
                                        onItemDeleted(timePartList.get(clickedTimePartIndex), clickedTimePartIndex, true, currentPosition.floatToPixel() % widthView)
                                    }
                                    timePartList.removeAt(clickedTimePartIndex)
                                }

                            }
                        }
                        TimePartParts.NOT_CLICKED -> {
                            currentMoveAction = MoveActions.SCROLLING
                            currentPosition += distanceXFloat
                        }
                    }
                }
                lastX = event.x
            }

            MotionEvent.ACTION_UP -> {
                firstTouchX = 0f
                firstTouchY = 0f
            }
        }

    }


    private fun moveTimePart(distanceXFloat: Float) {
        //logD("moveTimePart")
        currentMoveAction = MoveActions.MOVING

        val timePart = timePartList[clickedTimePartIndex]
        val startPositionFloat = timePart.start - distanceXFloat
        val endPositionFloat = timePart.end - distanceXFloat

        //logD("distanceXFloat $distanceXFloat startPositionFloat $startPositionFloat")

        var isPossible = true
        //check left and right time-parts if exist
        val prevIndex = clickedTimePartIndex - 1
        val nextIndex = clickedTimePartIndex + 1
        if (timePartClickedPart == TimePartParts.CENTER || timePartClickedPart == TimePartParts.LEFT_HANDLE) {
            if (prevIndex >= 0) {
                val prevTimePart = timePartList[prevIndex]
                if (prevTimePart.end > startPositionFloat) { //left side time-part end-value must be smaller than our start-value
                    isPossible = false
                }
            }
        }
        if (timePartClickedPart == TimePartParts.CENTER || timePartClickedPart == TimePartParts.RIGHT_HANDLE) {
            if (nextIndex < timePartList.size) {
                val nextTimePart = timePartList[nextIndex]
                if (endPositionFloat > nextTimePart.start) {
                    isPossible = false
                }
            }
        }

        //logD("isPossible $isPossible")
        if (!isPossible) return
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
        currentPosition = Math.min(maxTimePixel.pixelToFloat(), Math.max(0f, currentPosition))
        //logD("currentPosition $currentPosition maxTimePixel $maxTimePixel")
        currentTimeSecond = currentPosition.floatToSecond()
        if (timeTableListener != null) {
            timeTableListener!!.onTimeChanged(currentTimeSecond)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {

        canvas.drawColor(ColorBackground)

        drawRule(canvas)

        drawTimeParts(canvas)
    }

    override fun computeScroll() {
        //logD("computeScroll")
        if (currentMoveAction != MoveActions.MOVING && scroller.computeScrollOffset()) {
            currentPosition = scroller.currX.toFloat().pixelToFloat()
            computeTime()
        }
    }


    private fun drawRule(canvas: Canvas) {

        paint.color = seperatorColor
        paint.strokeWidth = seperatorWidth

        var floatSeconds = 0f
        var offset: Float = (0 - currentPosition.floatToPixel())
        for (i in 0..23) {

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
        val yPosition: Float = (heightView / 2).toFloat()
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
                    end - textPaintMaxWidth - textStartMargin,
                    heightView - seperatorTextTopMargin / 2,
                    textPaint!!
            )


            //draw center image
            timeTableListener?.getItemBitmap(timePart, index)?.let {
                val xStart = start + (end - start) / 2 - centerImageSize / 2
                val yStart = heightView / 2 - centerImageSize / 2
                canvas.drawBitmap(
                        it,
                        Rect(0, 0, centerImageSize, centerImageSize),
                        Rect(xStart.toInt(), yStart , ((xStart + centerImageSize).toInt()), yStart + centerImageSize),
                        paint
                )
            }

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

    fun addTimePart(timePart: TimePart) : Boolean {

        if (checkAddPossible(timePart)) {
            Toast.makeText(context, "ADDED", Toast.LENGTH_SHORT).show()
            logD("ADDED")
            timePartList.add(timePart)
            sortList()
            invalidate()
            return true
        } else {
            logD("NOT_POSSIBLE_ADDTIMEPART")
        }

        return false
    }

    fun checkAddPossible(timePart: TimePart): Boolean {
        var isPossible = true
        if (timePart.start < 0.0 || timePart.end >= 24.0) {
            isPossible = false
        }
        logD("timePartList.size ${timePartList.size}")
        logD("timePartList s${formatTimeHHmm(timePart.start)}e${formatTimeHHmm(timePart.end)}")
        timePartList.forEach {
            logD("${it.centerImage} s${formatTimeHHmm(it.start)}e${formatTimeHHmm(it.end)}")
            if (it.start < timePart.start && timePart.start < it.end
                    || it.start < timePart.end && timePart.end < it.end
                    || timePart.start < it.start && timePart.end > it.end) {
                isPossible = false
            }
        }

        return isPossible
    }

    fun addTimePartWithPosition(lengthFloat: Float, posX: Float, iconName: String) {

        val timePart = TimePart()
        timePart.start = currentPosition + posX.pixelToFloat() - (lengthFloat / 2)
        timePart.end = timePart.start + lengthFloat
        timePart.centerImage = iconName

        if (checkAddPossible(timePart)) {
            timePartList.add(timePart)
            sortList()
        } else {
            Toast.makeText(context, "NOT_POSSIBLE", Toast.LENGTH_SHORT).show()
        }

        timePartList.forEachIndexed { index, part ->
            //logD(" ***$index*** \n $part \n *****")
        }
        invalidate()
    }

    fun forceToAddTimePart(minuteLength: Int, posX: Float, iconName: String) {

        val lengthFloat = minuteLength.minuteToFloat()
        val timePart = TimePart()
        timePart.start = currentPosition + posX.pixelToFloat() - (lengthFloat / 2)
        timePart.end = timePart.start + lengthFloat
        timePart.centerImage = iconName


        if (timePart.start < 0.0) timePart.start = 0f
        if (timePart.end >= 24.0) timePart.end = TIME_END_MAX


        //logD("Original_ADD_PART ${formatTimeHHmm(timePart.start)}-${formatTimeHHmm(timePart.end)}")

        if (checkAddPossible(timePart)) {
            addTimePart(timePart)
            return
        } else {
            logD("NOT_POSSIBLE_1")
        }

        var prevIndex = -1
        for (i in 0 until timePartList.size) {
            val part = timePartList[i]
            if (part.start < timePart.start && timePart.start < part.end) {
                prevIndex = i
                break
            } else if (part.start < timePart.end && timePart.end < part.end) {
                prevIndex = i - 1
                break
            } else if (timePart.start < part.start && timePart.end > part.end) {
                prevIndex = i
                break
            }
        }
        logD("PREV_INDEX $prevIndex")

        var nextIndex = prevIndex + 1
        var prevPart = if (prevIndex > -1) timePartList[prevIndex] else null
        var nextPart = if (nextIndex < timePartList.size) timePartList[nextIndex] else null

        var canMinusLeftPart = 0f
        var canMinusRightPart = 0f

        val start = prevPart?.end ?: 0f
        val end: Float = nextPart?.start ?: TIME_END_MAX
        val totalEmptySpace = end - start

        logD("totalEmptySpace ${formatTimeHHmm(totalEmptySpace)} float:$totalEmptySpace")


        prevPart?.let {
            canMinusLeftPart = (it.end - it.start) - timePartMinFloat
        }

        nextPart?.let {
            canMinusRightPart = (it.end - it.start) - timePartMinFloat
        }

        val posSpace = totalEmptySpace + canMinusLeftPart + canMinusRightPart
        val itemNeededSpace: Float = if (totalEmptySpace - 60.minuteToFloat() > 0) totalEmptySpace - 60.minuteToFloat() else 0f
        logD("possible_space ${formatTimeHHmm(posSpace)} float:$posSpace")

        if (timePartMinFloat > posSpace) {
            logD("replace")
            if (prevPart != null) {
                timePartList.remove(prevPart)
                timePart.start = prevPart.start
                timePart.end = prevPart.end
                prevPart = null
                nextIndex--
            } else if (nextPart != null) {
                timePartList.remove(nextPart)
                timePart.start = nextPart.start
                timePart.end = nextPart.end
                nextPart = null
            }
        }

        //check left
        prevPart?.let {
            if (timePart.start < prevPart.end) {
                val diff = timePart.start - prevPart.start
                if (diff >= timePartMinFloat) {
                    //Can resize
                    timePartList[prevIndex].end = timePart.start
                } else {
                    timePartList[prevIndex].end = prevPart.start + timePartMinFloat
                    timePart.start = timePartList[prevIndex].end
                }
            }
        }

        //check right
        nextPart?.let {
            if (timePart.end > nextPart.start) {
                val diff = timePart.end - nextPart.end
                if (diff >= timePartMinFloat) {
                    //Can resize
                    timePartList[nextIndex].start = timePart.end
                } else {
                    timePartList[nextIndex].start = nextPart.end - timePartMinFloat
                    timePart.end = timePartList[nextIndex].start
                }
            }
        }


        logD("Modifyed_ADD_PART ${formatTimeHHmm(timePart.start)}-${formatTimeHHmm(timePart.end)}")
        addTimePart(timePart)
    }

    private fun sortList() {
        timePartList.sortWith(Comparator { p0, p1 ->
            (p0.start).compareTo(p1.end)
        })
    }

    fun clearAll() {
        timePartList.clear()
        invalidate()
    }


    fun setCurrentMinute(@FloatRange(from = 0.0, to = TIME_END_MAX.toDouble()) currentTimeFloat: Float) {
        this.currentTimeSecond = currentTimeFloat.floatToSecond()
        currentPosition = currentTimeFloat
        postInvalidate()
    }

    private fun stringTimeToSecond(currentTime: String) : Int {
        val array = currentTime.split(":")
        val hour = array[0].toInt()
        val minute = array[1].toInt()
        return  (hour * 60 * 60) + (minute * 60)
    }

    /**
     * send currentTime 01:23 format
     * 00:00 to 23:59
     */
    fun setCurrentTime(currentTime: String) {
        this.currentTimeSecond = stringTimeToSecond(currentTime)
        currentPosition = this.currentTimeSecond.secondToFloat()
        invalidate()
    }

    /**
     * getting now time and setted as starter time
     */
    fun setCurrentTimeNow() {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        val time = sdf.format(cal.time)
        this.currentTimeSecond = stringTimeToSecond(time)
        currentPosition = this.currentTimeSecond.secondToFloat()
        invalidate()
    }

    private fun formatTimeHHmm(@FloatRange(from = 0.0, to = 24.0) floatSecond: Float): String {

        val hour = floatSecond.toInt()
        val minute = TimeUnit.SECONDS.toMinutes((floatSecond - hour).floatToSecond().toLong())

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
        /**
         * seconds x min x hour = 1 day
         * 60 x 60 x 24 = 86400 seconds
         */
        const val MAX_TIME_VALUE: Int = 60 * 60 * 24
        /**
         * Our parts 1 hour long, 1 hour = 3600 seconds
         * 1 / 3600 = 0.000277777777778
         */
        const val FLOAT_CONSTANT = 0.000277777777778f

        /**
         * Time table view end value
         * time = 23:59
         */
        const val TIME_END_MAX = 23.999999f
    }

}

fun Int.secondToFloat(): Float {
    return this * TimeTableView.FLOAT_CONSTANT
}

fun Int.minuteToFloat(): Float {
    return TimeUnit.MINUTES.toSeconds(this.toLong()).toInt().secondToFloat()
}

fun Float.floatToSecond(): Int {
    return (this / TimeTableView.FLOAT_CONSTANT).toInt()
}

