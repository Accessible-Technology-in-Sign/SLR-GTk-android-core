package com.ccg.slrcore.common

/**
 * Configuration class for managing global constants and default values
 * related to hand tracking and prediction.
 *
 * <p>This class uses a companion object to store configuration values,
 * making them globally accessible. However, it is recommended to refactor
 * this into a builder pattern for better configurability and non-singleton
 * usage.</p>
 */
class Config {
    /**
     * Companion object containing all configuration constants.
     */
    companion object {
        /**
         * Name of the model asset file used for hand landmark detection.
         */
        var MODEL_ASSET = "hand_landmarker.task"

        /**
         * Confidence threshold for hand detection.
         */
        var HAND_DETECTION_CONFIDENCE = 0.5f

        /**
         * Confidence threshold for hand tracking.
         */
        var HAND_TRACKING_CONFIDENCE = 0.5f

        /**
         * Confidence threshold for determining hand presence in an image.
         */
        var HAND_PRESENCE_CONFIDENCE = 0.5f

        /**
         * Maximum number of hands to detect and track.
         */
        var NUM_HANDS = 1

        /**
         * Number of frames required to make a prediction.
         */
        var NUM_FRAMES_PREDICTION = 60

        /**
         * Number of input points (landmarks) used for hand detection.
         */
        var NUM_INPUT_POINTS = 21

        /**
         * Number of worker threads allocated for MediaPipe processing.
         */
        var NUM_WORKER_THREADS_MEDIAPIPE = 1

        /**
         * Number of worker threads allocated for TensorFlow Lite processing.
         */
        var NUM_WORKER_THREADS_TFLITE = 4
    }
}