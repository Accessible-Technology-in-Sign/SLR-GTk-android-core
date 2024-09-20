package com.ccg.slrcore.engine

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.camera.core.UseCase
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import com.ccg.slrcore.camera.CameraI
import com.ccg.slrcore.camera.StreamCameraXI
import com.ccg.slrcore.common.Config
import com.ccg.slrcore.model.CompleteBufferToTensor
import com.ccg.slrcore.model.MediapipeHandModelManager
import com.ccg.slrcore.model.PaddedBufferToTensor
import com.ccg.slrcore.model.SLRTfLiteModel
import com.ccg.slrcore.system.Buffer
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.io.File
import java.util.concurrent.Executors

class SimpleExecutionEngine(private val activity: Activity, onInit: SimpleExecutionEngine.() -> Unit = {}, onCycle: (String) -> Unit = {}) {
    private val executor = Executors.newSingleThreadExecutor()
    private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

    var isFrontCamera = true
    val cameraInterface: CameraI<UseCase,  ImageProxy> = StreamCameraXI(executor)
    val buffer = Buffer<HandLandmarkerResult>()
    var posePredictor = MediapipeHandModelManager(activity.baseContext, activity.resources)
    private var activelyPoll = false
    var isInterpolating = false

    fun poll() {
        this.activelyPoll = true
        buffer.clear()
        this.cameraInterface.poll(activity)
    }
    fun pause() {
        this.activelyPoll = false
        buffer.clear()
        this.cameraInterface.pause(activity)
    }

    val signPredictor = SLRTfLiteModel(
        File("${activity.cacheDir}/model")
            .apply { writeBytes(activity.assets.open("model_2.tflite").readBytes()) },
        activity.assets.open("signsList.txt")
            .bufferedReader().use{ reader ->
                reader.readText()
            }.split("\n")
    )

    init {
        requestAppPermissions()

        cameraInterface.addCallback(
            "mediapipe_hand"
        ) { img ->
            if (!activelyPoll) return@addCallback
            // convert image into bitmap and free imageproxy's image object
            val bitmapBuffer = img.toBitmap()
            // prepare required transformations to the image as described above
            posePredictor.single(BitmapImageBuilder(Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                Matrix().apply {
                    // rotation indicated by the proxy to correct image rotation
                    postRotate(img.imageInfo.rotationDegrees.toFloat())

                    // flip image if user use front camera
                    if (isFrontCamera) {
                        postScale(
                            -1f,
                            1f,
                            img.width.toFloat(),
                            img.height.toFloat()
                        )
                    }
                }, true
            )).build(), img.imageInfo.timestamp)
        }
        posePredictor.addCallback("buffer_fill") {
            mpResult ->
            if (mpResult.result.landmarks().size > 0)
                buffer.addElement(mpResult.result)
        }

        buffer.addCallback("asl_trigger") { bufferedResults ->
            val inputArray =
                mutableListOf<Float>() // the input points that can be sent to mediapipe

            if (isInterpolating && bufferedResults.size < Config.NUM_FRAMES_PREDICTION && bufferedResults.isNotEmpty()) {
                if (!PaddedBufferToTensor<HandLandmarkerResult>().createTensor(
                        bufferedResults,
                        inputArray
                    )
                ) {
                    return@addCallback
                }

            } else if (bufferedResults.isNotEmpty() && bufferedResults.size >= 60) {
                if (!CompleteBufferToTensor<HandLandmarkerResult>().createTensor(
                        bufferedResults,
                        inputArray
                    )
                ) {
                    return@addCallback
                }
            }

            if (inputArray.isNotEmpty()) {
                signPredictor.runModel(inputArray.toFloatArray())
            }
        }
        signPredictor.addCallback("user_cb") {
                sign -> onCycle(sign)
        }

        onInit()
    }

    private fun requestAppPermissions() {
        PERMISSIONS_REQUIRED.forEach {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(activity, arrayOf(it), 1)
            }
        }
    }
}