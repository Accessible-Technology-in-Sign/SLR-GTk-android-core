package com.ccg.slrcore.common

import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.LinkedList

/**
 * Utility class that provides predefined empty instances for various objects used in hand landmarking.
 *
 * <p>This class contains static constants for empty results and placeholder objects,
 * such as empty hand landmarking results and an empty bitmap.</p>
 *
 * @version 1.0.0
 */
class Empties {
    /**
     * A companion object holding static empty instances.
     */
    companion object {
        /**
         * An empty implementation of {@link HandLandmarkerResult} to be used as a default or placeholder.
         */
        val EMPTY_HANDMARKER_RESULTS = object: HandLandmarkerResult() {
            /**
             * Returns a default timestamp of 0.
             *
             * @return The default timestamp (0).
             */
            override fun timestampMs(): Long {
                return 0
            }

            /**
             * Returns an empty list of landmarks.
             *
             * @return An empty {@link LinkedList} of landmarks.
             */
            override fun landmarks(): MutableList<MutableList<NormalizedLandmark>> {
                return LinkedList()
            }

            /**
             * Returns an empty list of world landmarks.
             *
             * @return An empty {@link LinkedList} of world landmarks.
             */
            override fun worldLandmarks(): MutableList<MutableList<Landmark>> {
                return LinkedList()
            }

            /**
             * Returns an empty list of handedness categories.
             *
             * @return An empty {@link LinkedList} of handedness categories.
             */
            override fun handednesses(): MutableList<MutableList<Category>> {
                return LinkedList()
            }

        }

        /**
         * A predefined empty {@link MPImage} instance, initialized with a 1x1 ARGB_8888 bitmap.
         */
        val EMPTY_BITMAP: MPImage = BitmapImageBuilder(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)).build()
    }
}
