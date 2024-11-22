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

/**
 * Enum representing the modes for painting preview content.
 */
enum class PainterMode {
    /**
     * Paints only the image.
     */
    IMAGE_ONLY,

    /**
     * Paints only the skeleton of hand landmarks.
     */
    SKELETON_ONLY,

    /**
     * Paints both the image and the skeleton of hand landmarks.
     */
    IMAGE_AND_SKELETON
}

/**
 * Interface for defining methods to paint points, lines, and images in a preview.
 */
interface PreviewPainterInterface {
    /**
     * Paints a single point on the preview.
     *
     * @param x The x-coordinate of the point.
     * @param y The y-coordinate of the point.
     */
    fun paintPoint(x: Float, y: Float)


    /**
     * Paints multiple points on the preview.
     *
     * @param points A list of coordinate pairs representing the points to paint.
     */
    fun paintPoints(points: List<Pair<Float, Float>>)  {
        points.forEach {pair -> paintPoint(pair.first, pair.second)}
    }

    /**
     * Paints a line between two points on the preview.
     *
     * @param x1 The x-coordinate of the starting point.
     * @param y1 The y-coordinate of the starting point.
     * @param x2 The x-coordinate of the ending point.
     * @param y2 The y-coordinate of the ending point.
     */
    fun paintLine(x1: Float, y1: Float, x2: Float, y2: Float)

    /**
     * Paints an image on the preview.
     *
     * @param image The {@link Bitmap} image to paint.
     */
    fun paintImage(image: Bitmap)
}

/**
 * Class for managing the painting of hand landmarks and images in the preview.
 */
class HandPreviewPainter (private val painterInterface: PreviewPainterInterface, private val mode: PainterMode = PainterMode.IMAGE_AND_SKELETON) {
    /**
     * Paints the preview content based on the provided image, landmarks, and scaling factors.
     *
     * @param image         The {@link Bitmap} image to paint.
     * @param landmarkerResult The hand landmark detection result.
     * @param scaleFactorX  The scaling factor for the x-axis.
     * @param scaleFactorY  The scaling factor for the y-axis.
     */
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


/**
 * Implementation of {@link PreviewPainterInterface} using Jetpack Compose's {@link DrawScope}.
 */
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