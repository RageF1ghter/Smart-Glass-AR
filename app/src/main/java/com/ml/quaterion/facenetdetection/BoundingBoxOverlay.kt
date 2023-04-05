/*
 * Copyright 2021 Shubham Panchal
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ml.quaterion.facenetdetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.toRectF

// Defines an overlay on which the boxes and text will be drawn.
class BoundingBoxOverlay( context: Context , attributeSet: AttributeSet )
    : SurfaceView( context , attributeSet ) , SurfaceHolder.Callback {

    // Variables used to compute output2overlay transformation matrix
    // These are assigned in FrameAnalyser.kt
    var areDimsInit = false
    var frameHeight = 0
    var frameWidth = 0
//    private var orientation = Configuration.ORIENTATION_LANDSCAPE
    // This var is assigned in FrameAnalyser.kt
    var faceBoundingBoxes: ArrayList<Prediction>? = null


    private var output2OverlayTransform: Matrix = Matrix()

    // Paint for boxes and text
    private val boxPaint = Paint().apply {
        strokeWidth = 2.0f
        color = Color.BLUE
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint().apply {
        strokeWidth = 5.0f
        textSize = 50f
        color = Color.RED
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        TODO("Not yet implemented")
    }


    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }


    override fun surfaceDestroyed(holder: SurfaceHolder) {
        TODO("Not yet implemented")
    }


    override fun onDraw(canvas: Canvas?) {
        if (faceBoundingBoxes != null) {
            if (!areDimsInit) {
                val viewWidth = canvas!!.width.toFloat()
                val viewHeight = canvas.height.toFloat()
                val xFactor: Float = viewWidth / frameWidth.toFloat()
                val yFactor: Float = viewHeight / frameHeight.toFloat()
                // Scale and mirror the coordinates ( required for front lens )
                output2OverlayTransform.preScale(xFactor, yFactor)
                output2OverlayTransform.postScale(-1f, 1f, viewWidth / 2f, viewHeight / 2f)
                areDimsInit = true
            }
            else {
                for (face in faceBoundingBoxes!!) {
                    val boundingBox = face.bbox.toRectF()
                    output2OverlayTransform.mapRect(boundingBox)
                    canvas?.drawRoundRect(boundingBox, 16f, 16f, boxPaint)
                    canvas?.drawText(
                        face.label,
                        boundingBox.left,
                        boundingBox.top,
                        textPaint
                    )
                }
            }
        }
    }

}
