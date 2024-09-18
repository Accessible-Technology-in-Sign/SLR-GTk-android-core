package com.ccg.slrcore.common

import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.LinkedList

class Empties {
    companion object {

        val EMPTY_HANDMARKER_RESULTS = object: HandLandmarkerResult() {
            override fun timestampMs(): Long {
                return 0
            }

            override fun landmarks(): MutableList<MutableList<NormalizedLandmark>> {
                return LinkedList()
            }

            override fun worldLandmarks(): MutableList<MutableList<Landmark>> {
                return LinkedList()
            }

            override fun handednesses(): MutableList<MutableList<Category>> {
                return LinkedList()
            }

        }

        val EMPTY_BITMAP: MPImage = BitmapImageBuilder(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)).build()
    }
}
