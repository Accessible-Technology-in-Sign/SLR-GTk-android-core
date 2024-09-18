package com.ccg.slrcore.model

import android.content.Context
import android.content.res.Resources
import com.ccg.slrcore.common.Config
import com.ccg.slrcore.common.Empties
import com.ccg.slrcore.common.ImageMPResultWrapper
import com.ccg.slrcore.common.SLREventHandler
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.lang.RuntimeException

class MediapipeHandModelManager {
    private val graph: HandLandmarker
    private val callbacks: HashMap<String, SLREventHandler<ImageMPResultWrapper<HandLandmarkerResult>>> = HashMap()
    private val errorCallbacks: HashMap<String, SLREventHandler<RuntimeException>> = HashMap()

    private val outputInputLookup: HashMap<Long, MPImage> = HashMap()
    private val runningMode: RunningMode
    private val context: Context
    private val resources: Resources
    constructor(context: Context, resources: Resources): this(context, resources, RunningMode.LIVE_STREAM)
    constructor(context: Context, resources: Resources, runningMode: RunningMode) {
        this.context = context
        this.resources = resources
        this.runningMode = runningMode
        graph = HandLandmarker.createFromOptions(context, HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(Config.MODEL_ASSET).build())
            .setMinHandDetectionConfidence(Config.HAND_DETECTION_CONFIDENCE)
            .setMinTrackingConfidence(Config.HAND_TRACKING_CONFIDENCE)
            .setMinHandPresenceConfidence(Config.HAND_PRESENCE_CONFIDENCE)
            .setNumHands(Config.NUM_HANDS)
            .also {
                    options ->
                when (runningMode) {
                    RunningMode.LIVE_STREAM -> options.setResultListener{
                            i, _ ->
                        callbacks.forEach {
                                (_, cb) ->
                            cb.handle(
                                ImageMPResultWrapper(
                                    i,
                                    outputInputLookup.getOrDefault(i.timestampMs(), Empties.EMPTY_BITMAP)
                                )
                            )
                        }
                        outputInputLookup.remove(i.timestampMs())
                    }
                    else -> {}
                }
            }
            .setErrorListener{i-> errorCallbacks.forEach { (_, cb) -> cb.handle(i)}}
            .setRunningMode(runningMode)
            .build()
        )
    }

    fun single(img: MPImage, timestamp: Long) {
        when (runningMode) {
            RunningMode.LIVE_STREAM -> {
                outputInputLookup[timestamp] = img
                graph.detectAsync(img, timestamp)
            }
            RunningMode.IMAGE -> {
                val result = graph.detect(img)
                callbacks.forEach {
                    (_, cb) ->
                    cb.handle(ImageMPResultWrapper(result, img))
                }
            }
            RunningMode.VIDEO -> {
                val result = graph.detectForVideo(img, timestamp)
                callbacks.forEach {
                        (_, cb) ->
                    cb.handle(ImageMPResultWrapper(result, img))
                }
            }
        }
    }

    fun addCallback(name: String, callback: SLREventHandler<ImageMPResultWrapper<HandLandmarkerResult>>) {
        this.callbacks[name] = callback
    }

    fun removeCallback(name: String) {
        if (name in this.callbacks) this.callbacks.remove(name)
    }

    fun addErrorCallback(name: String, callback: SLREventHandler<RuntimeException>) {
        this.errorCallbacks[name] = callback
    }

    fun removeErrorCallback(name: String) {
        if (name in this.errorCallbacks) this.callbacks.remove(name)
    }
}