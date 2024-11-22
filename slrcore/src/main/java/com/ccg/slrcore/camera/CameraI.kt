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

/**
 * Interface representing a camera implementation with generic use cases and callbacks.
 *
 * @param <T> Type representing the use case.
 * @param <U> Type representing the callback data.
 */
interface CameraI<T, U> {
    /**
     * Starts the camera and binds the use cases to the lifecycle.
     *
     * @param context The context of the calling component.
     */
    fun poll(context: Context)

    /**
     * Stops the camera and unbinds all use cases.
     *
     * @param context The context of the calling component.
     */
    fun pause(context: Context)

    /**
     * Adds a new use case to the camera.
     *
     * @param name    Name of the use case.
     * @param useCase The use case to be added.
     */
    fun addUseCase(name: String, useCase: T)

    /**
     * Removes a use case from the camera.
     *
     * @param name Name of the use case to be removed.
     */
    fun removeUseCase(name: String)

    /**
     * Adds a callback for handling camera events.
     *
     * @param name      Name of the callback.
     * @param callbacks The callback handler.
     */
    fun addCallback(name: String, callbacks: SLREventHandler<U>)

    /**
     * Removes a callback handler.
     *
     * @param name Name of the callback to be removed.
     */
    fun removeCallback(name: String)

}

/**
 * Properties for configuring the behavior of the camera.
 */
class CameraIProperties(
    val enableISO: Boolean = false,
    val cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
)

/**
 * Base implementation of {@link CameraI} for managing camera use cases and callbacks.
 */
open class CameraXI: CameraI<UseCase, ImageProxy> {
    protected val callbacks: HashMap<String, SLREventHandler<ImageProxy>> = HashMap()

    private val useCases: HashMap<String, UseCase> = HashMap()
    private var properties: CameraIProperties

    /**
     * Default constructor initializes with default properties.
     */
    constructor() {
        this.properties = CameraIProperties()
    }

    /**
     * Constructor initializes with custom properties.
     *
     * @param properties Properties to configure the camera.
     */
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
/**
 * Specialized implementation of {@link CameraXI} for managing real-time stream callbacks.
 */
@OptIn(ExperimentalCamera2Interop::class)

class StreamCameraXI: CameraXI {
    /**
     * Constructor initializes with default properties and executor.
     *
     * @param executor Executor for running image callbacks.
     */
    constructor(executor: Executor): this(CameraIProperties(), executor)

    /**
     * Constructor initializes with custom properties and executor.
     *
     * @param properties Custom properties for configuring the camera.
     * @param executor   Executor for running image callbacks.
     */
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