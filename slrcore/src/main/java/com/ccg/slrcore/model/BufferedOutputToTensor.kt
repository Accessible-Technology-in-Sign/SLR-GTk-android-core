package com.ccg.slrcore.model

import com.ccg.slrcore.common.Config
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

fun interface BufferToTensor<T : HandLandmarkerResult> {
    fun createTensor(buffer: List<T>, tensor: MutableList<Float>): Boolean
}

class PaddedBufferToTensor<T : HandLandmarkerResult>: BufferToTensor<T> {
    override fun createTensor(buffer: List<T>, tensor: MutableList<Float>): Boolean {
        buffer.forEach{ landmark ->
            for (j in 0..<Config.NUM_INPUT_POINTS) { // we it
                if (landmark.landmarks().size <= 0 || landmark.landmarks()[0].size <= 0) // if we don't have a landmark at a given frame
                    return false // currently we exit since a pre-requisite to the model working, i think, is that all frames have a landmark. //TODO: verify this assumption
                tensor.add(landmark.landmarks()[0][j].x()) // add the x and y coordinates on the list as per the format required by the model
                tensor.add(landmark.landmarks()[0][j].y())
            }
        }

        val midpoint = buffer[buffer.size / 2]

        repeat (60 - buffer.size) {
            for (j in 0..<Config.NUM_INPUT_POINTS) { // we it
                if (midpoint.landmarks().size <= 0 || midpoint.landmarks()[0].size <= 0) // if we don't have a landmark at a given frame
                    return false // currently we exit since a pre-requisite to the model working, i think, is that all frames have a landmark. //TODO: verify this assumption
                tensor.add(midpoint.landmarks()[0][j].x()) // add the x and y coordinates on the list as per the format required by the model
                tensor.add(midpoint.landmarks()[0][j].y())
            }
        }

        return true
    }
}

class CompleteBufferToTensor<T : HandLandmarkerResult>: BufferToTensor<T> {
    override fun createTensor(buffer: List<T>, tensor: MutableList<Float>): Boolean {
        buffer
            .subList(buffer.size - Config.NUM_FRAMES_PREDICTION, buffer.size) // last NUM_FRAMES_PREDICTION = 60 frames in the buffer
            .forEach { landmark -> // we take each result landmark
                for (j in 0..<Config.NUM_INPUT_POINTS) { // we iterate over all 21 hand landmark points
                    if (landmark.landmarks().size <= 0 || landmark.landmarks()[0].size <= 0) // if we don't have a landmark at a given frame
                        return false // currently we exit since a pre-requisite to the model working, i think, is that all frames have a landmark. //TODO: verify this assumption
                    tensor.add(landmark.landmarks()[0][j].x()) // add the x and y coordinates on the list as per the format required by the model
                    tensor.add(landmark.landmarks()[0][j].y())
                }
            }

        return true
    }
}