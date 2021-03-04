package com.murgupluoglu.timetablelib

import android.graphics.Canvas
import android.graphics.Point
import android.view.View

class EmptyDragShadowBuilder(arg0: View) : View.DragShadowBuilder(arg0) {

    override fun onProvideShadowMetrics(size: Point, touch: Point) {
        super.onProvideShadowMetrics(size, touch)
        size.set(1, 1)
        touch.set(0, 0)
    }

    override fun onDrawShadow(canvas: Canvas) {
        // draw nothing
    }
}