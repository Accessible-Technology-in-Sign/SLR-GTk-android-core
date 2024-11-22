package com.ccg.slrcore.model

import com.ccg.slrcore.common.Config
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * Functional interface for converting a buffer of hand landmarks into a tensor representation.
 *
 * @param <T> The type of the elements in the buffer, extending {@link HandLandmarkerResult}.
 */
fun interface BufferToTensor<T : HandLandmarkerResult> {
    /**
     * Converts a buffer of hand landmarks into a tensor representation.
     *
     * @param buffer The list of hand landmark results to convert.
     * @param tensor The mutable list to store the converted tensor data.
     * @return {@code true} if the conversion was successful, {@code false} otherwise.
     */
    fun createTensor(buffer: List<T>, tensor: MutableList<Float>): Boolean
}

/**
 * Converts a partially filled buffer of hand landmarks into a tensor representation, padding it if necessary.
 *
 * @param <T> The type of the elements in the buffer, extending {@link HandLandmarkerResult}.
 */
class PaddedBufferToTensor<T : HandLandmarkerResult>: BufferToTensor<T> {
    /**
     * Converts a buffer of hand landmarks into a tensor, padding the buffer with data
     * from the midpoint frame if the buffer size is smaller than required.
     *
     * @param buffer The list of hand landmark results to convert.
     * @param tensor The mutable list to store the converted tensor data.
     * @return {@code true} if the conversion was successful, {@code false} if any frame lacked landmarks.
     */
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

/**
 * Converts a fully filled buffer of hand landmarks into a tensor representation,
 * using the last {@link Config#NUM_FRAMES_PREDICTION} frames from the buffer.
 *
 * @param <T> The type of the elements in the buffer, extending {@link HandLandmarkerResult}.
 */
class CompleteBufferToTensor<T : HandLandmarkerResult>: BufferToTensor<T> {
    /**
     * Converts the last {@link Config#NUM_FRAMES_PREDICTION} frames from the buffer into a tensor.
     *
     * @param buffer The list of hand landmark results to convert.
     * @param tensor The mutable list to store the converted tensor data.
     * @return {@code true} if the conversion was successful, {@code false} if any frame lacked landmarks.
     */
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