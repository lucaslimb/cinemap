package lucaslimb.com.github.cinemap.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.widget.OverScroller
import lucaslimb.com.github.cinemap.R
import kotlin.math.abs
import kotlin.math.roundToInt

class TimelineSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paintLine = Paint().apply {
        color = resources.getColor(R.color.slider_grey)
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val paintTick = Paint().apply {
        color = resources.getColor(R.color.slider_grey)
        strokeWidth = 6f
        isAntiAlias = true
    }

    var startYear = 1880 - 12
    var endYear = 2025

    var onYearSelected: ((Int) -> Unit)? = null

    var onYearSettled: ((Int) -> Unit)? = null

    var selectedYear = startYear

    private var scrollOffset = 0f

    private var lastTouchX = 0f

    private var isScrolling = false

    private var scrollSensitivity = 1.8f

    private val scroller = OverScroller(context)
    private val flingRunnable = object : Runnable {
        override fun run() {
            if (scroller.computeScrollOffset()) {
                scrollOffset = scroller.currX.toFloat()
                invalidate()
                postOnAnimation(this)
                updateSelectedYear()
            } else {
                if (!isScrolling) {
                    snapToNearestTick()
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val spacing = 40f
        val centerY = height / 2f
        val minScrollOffset = -(width / 2f)
        val maxScrollOffset = (endYear - startYear) * spacing - (width / 2f)
        scrollOffset = scrollOffset.coerceIn(minScrollOffset, maxScrollOffset)

        canvas.drawLine(0f, centerY, width.toFloat(), centerY, paintLine)

        for (year in 1880..endYear) {
            val absoluteIndex = year - startYear
            val x = absoluteIndex * spacing - scrollOffset
            if (x in -spacing..(width + spacing)) {
                val isTallTick = (year) % 10 == 0
                val tickHeight = if (isTallTick) 120f else 60f
                val top = centerY - tickHeight / 2
                val bottom = centerY + tickHeight / 2
                canvas.drawLine(x, top, x, bottom, paintTick)
            }
        }
    }

    private fun updateSelectedYear() {
        val spacing = 40f
        val centerX = width / 2f

        val absoluteTickIndex = ((scrollOffset + centerX) / spacing).roundToInt()
        val newYear = (startYear + absoluteTickIndex).coerceIn(1880, endYear)

        if (newYear != selectedYear) {
            selectedYear = newYear
            onYearSelected?.invoke(newYear)
        }
    }

    private fun snapToNearestTick() {
        val spacing = 40f
        val centerX = width / 2f

        val currentYearAtCenter = ((scrollOffset + centerX) / spacing).roundToInt() + startYear
        val targetYear = currentYearAtCenter.coerceIn(1880, endYear)
        val targetAbsoluteIndex = targetYear - startYear
        val targetOffset = targetAbsoluteIndex * spacing - centerX
        val minOffset = -(width / 2f)
        val maxOffset = (endYear - startYear) * spacing - (width / 2f)
        val clampedOffset = targetOffset.coerceIn(minOffset, maxOffset)
        if (abs(scrollOffset - clampedOffset) > 1) {
            scroller.startScroll(
                scrollOffset.toInt(), 0,
                (clampedOffset - scrollOffset).toInt(), 0,
                200
            )
            postOnAnimation(flingRunnable)
        } else {
            if (targetYear != selectedYear) {
                selectedYear = targetYear
                onYearSelected?.invoke(targetYear)
            }
            onYearSettled?.invoke(selectedYear)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initializePosition()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                scroller.forceFinished(true)
                lastTouchX = event.x
                isScrolling = true
                removeCallbacks(flingRunnable)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = lastTouchX - event.x
                scrollOffset += dx * scrollSensitivity
                lastTouchX = event.x
                invalidate()
                updateSelectedYear()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isScrolling = false
                val velocityTracker = VelocityTracker.obtain()
                velocityTracker.addMovement(event)
                velocityTracker.computeCurrentVelocity(1000)
                val velocityX = velocityTracker.xVelocity
                velocityTracker.recycle()
                val spacing = 40f
                val centerX = width / 2f
                val minFlingX = -(width / 2f).toInt()
                val maxFlingX = ((endYear - startYear) * spacing - (width / 2f)).toInt()
                if (abs(velocityX) > 50) {
                    scroller.fling(
                        scrollOffset.toInt(), 0,
                        velocityX.toInt(), 0,
                        minFlingX, maxFlingX,
                        0, 0,
                        width / 2, 0
                    )
                } else {
                    val currentYearAtCenter = ((scrollOffset + centerX) / spacing).roundToInt() + startYear
                    val targetYear = currentYearAtCenter.coerceIn(1880, endYear)
                    val targetAbsoluteIndex = targetYear - startYear
                    val targetOffset = targetAbsoluteIndex * spacing - centerX

                    val clampedTargetOffset = targetOffset.coerceIn(minFlingX.toFloat(), maxFlingX.toFloat())

                    scroller.startScroll(
                        scrollOffset.toInt(), 0,
                        (clampedTargetOffset - scrollOffset).toInt(), 0,
                        200
                    )
                }
                postOnAnimation(flingRunnable)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun initializePosition() {
        if (width == 0) {
            post { initializePosition() }
            return
        }

        val spacing = 40f
        val centerX = width / 2f
        val targetYear = 1970
        val targetAbsoluteIndex = targetYear - startYear
        scrollOffset = targetAbsoluteIndex * spacing - centerX

        updateSelectedYear()
        snapToNearestTick()
        invalidate()
    }
}
