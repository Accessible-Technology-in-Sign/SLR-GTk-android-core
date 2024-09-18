package com.ccg.slrcore.common

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.ui.geometry.Size
import com.google.mediapipe.framework.image.BitmapExtractor
import com.google.mediapipe.framework.image.MPImage


/***
 * ImageMPResultWrapper is a dataclass for a MPResult like HandLandmarks and a MediapipeImage which is the corresponding input
 * @author Ananay Vikram Gupta
 * @version 1.0.0
 */
class ImageMPResultWrapper<T>(val result: T, val image: MPImage) {
    fun getBitmap(size: Size): Bitmap {
        val resultBmp = BitmapExtractor.extract(image)
        val targetAR = maxOf(size.width / resultBmp.width, size.height / resultBmp.height)

        val img =
            Bitmap.createBitmap(resultBmp, 0, 0, resultBmp.width, resultBmp.height, Matrix().also {
                it.setRectToRect(
                    RectF(0f, 0f, resultBmp.width.toFloat(), resultBmp.height.toFloat()), RectF(
                        0f, 0f, resultBmp.width * targetAR,
                        resultBmp.height * targetAR
                    ), Matrix.ScaleToFit.FILL
                )
            }, true)

        return img
    }
}