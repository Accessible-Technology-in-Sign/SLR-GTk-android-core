package com.ccg.slrcore.preview

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ccg.slrcore.common.Empties
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

enum class PainterMode {
    IMAGE_ONLY,
    SKELETON_ONLY,
    IMAGE_AND_SKELETON
}

interface PreviewPainterInterface {
    fun paintPoint(x: Float, y: Float)
    fun paintPoints(points: List<Pair<Float, Float>>)  {
        points.forEach {pair -> paintPoint(pair.first, pair.second)}
    }
    fun paintLine(x1: Float, y1: Float, x2: Float, y2: Float)
    fun paintImage(image: Bitmap)
}

class HandPreviewPainter (private val painterInterface: PreviewPainterInterface, private val mode: PainterMode = PainterMode.IMAGE_AND_SKELETON) {
        fun paint(image: Bitmap?, landmarkerResult: HandLandmarkerResult?, scaleFactorX: Float, scaleFactorY: Float) {
        if (image != null && (mode == PainterMode.IMAGE_ONLY || mode == PainterMode.IMAGE_AND_SKELETON))
            painterInterface.paintImage(image)
        if (landmarkerResult != Empties.EMPTY_HANDMARKER_RESULTS && (mode == PainterMode.SKELETON_ONLY || mode == PainterMode.IMAGE_AND_SKELETON)) {
            landmarkerResult!!.landmarks().forEach { normalizedLandmarks ->
                painterInterface.paintPoints(normalizedLandmarks.map {
                    Pair(it.x() * scaleFactorX, it.y() * scaleFactorY)
                })
            }
            if (landmarkerResult.landmarks().size > 0)
                HandLandmarker.HAND_CONNECTIONS.forEach {
                    painterInterface.paintLine(
                        landmarkerResult.landmarks()[0][it!!.start()]
                            .x() * scaleFactorX,
                        landmarkerResult.landmarks()[0][it.start()]
                            .y() * scaleFactorY,
                        landmarkerResult.landmarks()[0][it.end()]
                            .x() * scaleFactorX,
                        landmarkerResult.landmarks()[0][it.end()]
                            .y() * scaleFactorY,
                    )
                }
        }
    }
}

class ComposeCanvasPainterInterface(
    private val scope: DrawScope
    ): PreviewPainterInterface {
    override fun paintImage(image: Bitmap) {
        val img  = image.asImageBitmap()
        if (img != Empties.EMPTY_BITMAP) {
            scope.drawImage(
                img,
                Offset(
                    scope.center.x - scope.size.width / 2,
                    scope.center.y - scope.size.height / 2
                )
            )
        }
    }

    override fun paintLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        scope.drawLine(
            Color.Red,
            Offset(
                x1, y1
            ),
            Offset(
                x2, y2
            ),
            strokeWidth = 10F
        )
    }

    override fun paintPoint(x: Float, y: Float) {
        return
    }

    override fun paintPoints(points: List<Pair<Float, Float>>) {
        scope.drawPoints(
            points.map { Offset(it.first, it.second) },
            PointMode.Points,
            Color.Black,
            strokeWidth = 10F
        )
    }
}