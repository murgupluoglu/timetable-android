package com.murgupluoglu.timetablelib

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.util.Log
import android.view.View


class ImageDragShadowBuilder(val context: Context, val shadow: View) : View.DragShadowBuilder() {

    override fun onDrawShadow(canvas: Canvas) {
        shadow.draw(canvas)
    }

    override fun onProvideShadowMetrics(shadowSize: Point, shadowTouchPoint: Point) {
        Log.e("ImageDragShadowBuilder", "onProvideShadowMetrics")
        shadowSize.x = shadow.width
        shadowSize.y = shadow.height

        shadowTouchPoint.x = shadowSize.x / 2
        shadowTouchPoint.y = shadowSize.y / 2 + 200
    }
}