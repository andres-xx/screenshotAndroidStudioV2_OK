/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.baseui.overlayviews.condition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.buzbuz.smartautoclicker.extensions.ScreenMetrics
import com.buzbuz.smartautoclicker.extensions.scale
import com.buzbuz.smartautoclicker.extensions.translate
import kotlin.math.max
import kotlin.math.min

internal class Capture(
    context: Context,
    screenMetrics: ScreenMetrics,
    viewInvalidator: () -> Unit,
): SelectorViewComponent(screenMetrics, viewInvalidator) {

    private companion object {
        /** The minimum zoom value. */
        private const val ZOOM_MINIMUM = 0.8f
        /** The maximum zoom value. */
        private const val ZOOM_MAXIMUM = 3f
    }

    /** */
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            translateCapture(-distanceX, -distanceY)
            return true
        }
    }
    /** */
    private val gestureDetector = GestureDetector(context, gestureListener)

    /** */
    private val scaleGestureListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        /** */
        private val scaleFocus = PointF()

        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            scaleFocus.apply {
                x = scaleGestureDetector.focusX
                y = scaleGestureDetector.focusY
            }
            scaleCapture(scaleGestureDetector.scaleFactor, scaleFocus)

            return true
        }
    }
    /** */
    private val scaleGestureDetector = ScaleGestureDetector(context, scaleGestureListener)

    /** The drawable for the screen capture. */
    var screenCapture: BitmapDrawable? = null
    /** The current zoom level*/
    var zoomLevel = 1f
        private set
    /** The current area where the capture is displayed. It can be bigger than the screen when zoomed. */
    val captureArea = RectF(0f, 0f, maxArea.width(), maxArea.height())

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = gestureDetector.onTouchEvent(event)
        handled = handled or scaleGestureDetector.onTouchEvent(event)
        return handled
    }

    private fun translateCapture(translateX: Float, translateY: Float) {
        // Verify if the translation isn't moving the capture too far away and correct the value to avoid to "lost" the
        // capture.
        var inboundsTranslateX = translateX
        var inboundsTranslateY = translateY

        val horizontalMargin = maxArea.width() * 0.2f
        val verticalMargin = maxArea.height() * 0.2f

        if (translateX > 0 && captureArea.left + translateX > maxArea.right - horizontalMargin) {
            inboundsTranslateX = min(0f, translateX)
        } else if (translateX < 0 && captureArea.right + translateX < maxArea.left + horizontalMargin) {
            inboundsTranslateX = max(0f, translateX)
        }
        if (translateY > 0 && captureArea.top + translateY > maxArea.bottom - verticalMargin) {
            inboundsTranslateY= min(0f, translateY)
        } else if (translateY < 0 && captureArea.bottom - translateY < maxArea.top + verticalMargin) {
            inboundsTranslateY = max(0f, translateY)
        }

        // Translate safely
        captureArea.translate(inboundsTranslateX, inboundsTranslateY)

        invalidate()
    }

    /**
     *
     */
    fun setZoomLevel(newLevel: Float) {
        scaleCapture(
            scaleFactor = newLevel / zoomLevel,
            scalePivot = PointF(captureArea.centerX(), captureArea.centerY())
        )
    }

    private fun scaleCapture(scaleFactor: Float, scalePivot: PointF) {
        val newZoom = (zoomLevel * scaleFactor).coerceIn(ZOOM_MINIMUM, ZOOM_MAXIMUM)
        if (zoomLevel == newZoom) {
            return
        }
        zoomLevel = newZoom

        val pivot = if (newZoom < 1) {
            PointF(maxArea.centerX(), maxArea.centerY())
        } else {
            scalePivot
        }
        captureArea.scale(scaleFactor, pivot)

        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        screenCapture?.apply {
            canvas.drawColor(Color.BLACK)
            setBounds(
                captureArea.left.toInt(),
                captureArea.top.toInt(),
                captureArea.right.toInt(),
                captureArea.bottom.toInt()
            )
            draw(canvas)
        }
    }
}