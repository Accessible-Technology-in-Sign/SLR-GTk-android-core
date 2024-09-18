package com.ccg.slrcore.common

// TODO: this should not be a singleton, make a builder?
class Config {
    companion object {
        var MODEL_ASSET = "hand_landmarker.task"
        var HAND_DETECTION_CONFIDENCE = 0.5f
        var HAND_TRACKING_CONFIDENCE = 0.5f
        var HAND_PRESENCE_CONFIDENCE = 0.5f
        var NUM_HANDS = 1
        var NUM_FRAMES_PREDICTION = 60
        var NUM_INPUT_POINTS = 21
        var NUM_WORKER_THREADS_MEDIAPIPE = 1
        var NUM_WORKER_THREADS_TFLITE = 4
    }
}