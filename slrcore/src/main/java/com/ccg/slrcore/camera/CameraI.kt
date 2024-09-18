package com.ccg.slrcore.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CaptureRequest
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import com.ccg.slrcore.common.SLREventHandler
import java.util.concurrent.Executor

interface CameraI<T, U> {
    fun poll(context: Context)
    fun pause(context: Context)
    fun addUseCase(name: String, useCase: T)
    fun removeUseCase(name: String)
    fun addCallback(name: String, callbacks: SLREventHandler<U>)
    fun removeCallback(name: String)

}

class CameraIProperties(
    val enableISO: Boolean = false,
    val cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
)

open class CameraXI: CameraI<UseCase, ImageProxy> {
    protected val callbacks: HashMap<String, SLREventHandler<ImageProxy>> = HashMap()

    private val useCases: HashMap<String, UseCase> = HashMap()
    private var properties: CameraIProperties

    constructor() {
        this.properties = CameraIProperties()
    }
    constructor(properties: CameraIProperties) {
        this.properties = properties
    }
    override fun addUseCase(name: String, useCase: UseCase) {
        this.useCases[name] = useCase
    }
    override fun removeUseCase(name: String) {
        if (name in this.useCases) this.useCases.remove(name)
    }
    @SuppressLint("RestrictedApi")
    override fun poll(context: Context) {
        val cameraPromise = ProcessCameraProvider
            .getInstance(context)
        cameraPromise.addListener({
            cameraPromise
                .get()
                .also { it.unbindAll() }
                .also {
                    cameraProvider ->
                    useCases.forEach { (_, useCase) ->
                        cameraProvider.bindToLifecycle(
                            ProcessLifecycleOwner.get(),
                            properties.cameraSelector,
                            useCase
                        )
                    }
                }
        }, ContextCompat.getMainExecutor(context))
    }
    override fun pause(context:  Context) {
        val cameraPromise = ProcessCameraProvider
            .getInstance(context)
        cameraPromise.addListener({
            cameraPromise
                .get()
                .unbindAll()
        }, ContextCompat.getMainExecutor(context))
    }


    override fun addCallback(name: String, callbacks: SLREventHandler<ImageProxy>) {
        this.callbacks[name] = callbacks
    }
    override fun removeCallback(name: String) {
        if (name in this.callbacks) this.callbacks.remove(name)
    }
}
@OptIn(ExperimentalCamera2Interop::class)

class StreamCameraXI: CameraXI {
    constructor(executor: Executor): this(CameraIProperties(), executor)
    constructor(properties: CameraIProperties, executor: Executor): super(properties) {
        super.addUseCase("__internal_stream_callbacks", ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .also {
                Camera2Interop
                    .Extender(it)
                    .setCaptureRequestOption(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        if (properties.enableISO) CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                        else CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
                    )
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        if (properties.enableISO) CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                        else CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                    )
            }
            .build()
            .also {
                it.setAnalyzer(executor) {
                        image ->
                    callbacks.forEach { (_, cb) -> cb.handle(image)}
                    image.close()
                }
            }
        )
    }

    override fun addUseCase(name: String, useCase: UseCase) {
        if (name == "__internal_stream_callbacks") return
        super.addUseCase(name, useCase)
    }
    override fun removeUseCase(name: String) {
        if (name == "__internal_stream_callbacks") return
        super.removeUseCase(name)
    }

}