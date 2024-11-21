package com.ccg.slrcore.model

import com.ccg.slrcore.common.Config
import com.ccg.slrcore.common.FilterUnit
import com.ccg.slrcore.common.PassThroughFilterSingle
import com.ccg.slrcore.common.PredictionFilter
import com.ccg.slrcore.common.SLREventHandler
import com.ccg.slrcore.common.argmax
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.util.LinkedList

/**
 * Abstract base class for managing TensorFlow Lite models.
 *
 * This class initializes the TensorFlow Lite {@link Interpreter} with the provided model
 * file and configuration options.
 */
abstract class TfLiteModelManager(
        model: File,
    ) {

    /**
     * The TensorFlow Lite interpreter used to run inference on the model.
     */
    protected val interpreter: Interpreter = Interpreter(
        model,
        Interpreter.Options().apply{ this.setNumThreads(Config.NUM_WORKER_THREADS_MEDIAPIPE) }
    )
}

/**
 * Class for managing and running ASL predictions using a TensorFlow Lite model.
 *
 * @param <T> The type of the elements in the mapping used for predictions.
 */
class SLRTfLiteModel <T> (
    model: File,
    val mapping: List<T>
): TfLiteModelManager(model) {
    private val modelInputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 60, 21 * 2, 1), DataType.FLOAT32)
    private val modelOutputTensor = TensorBuffer.createFixedSize(interpreter.getOutputTensor(0).shape(), DataType.FLOAT32)

    /**
     * List of filters to apply to the model output.
     */
    var outputFilters: MutableList<PredictionFilter<T>> = LinkedList<PredictionFilter<T>>().also { it.add(PassThroughFilterSingle()) }


    /**
     * Runs the model with the provided input tensor data.
     *
     * @param inputArray A float array containing the input tensor data, formatted as per the model's requirements.
     */
    fun runModel(inputArray: FloatArray) {
        modelInputTensor.loadArray(inputArray)
        interpreter.run(modelInputTensor.buffer,  modelOutputTensor.buffer)
        var values = FilterUnit(mapping,modelOutputTensor.floatArray)
        for (filter in outputFilters)
            values = filter.filter(values)
        callbacks.forEach {
            (_, cb) ->
            if (values.mapping.size == 1)
                cb.handle(values.mapping[0])
            else if (values.mapping.size > 1)
                cb.handle(values.mapping[values.probabilities.argmax()])
        }
    }

    private val callbacks: HashMap<String, SLREventHandler<T>> = HashMap()

    /**
     * Adds a callback to handle prediction results.
     *
     * @param name     The name of the callback.
     * @param callback The callback handler to execute when predictions are made.
     */
    fun addCallback(name: String, callback: SLREventHandler<T>) {
        this.callbacks[name] = callback
    }

    /**
     * Removes a callback from the model manager.
     *
     * @param name The name of the callback to remove.
     */
    fun removeCallback(name: String) {
        if (name in this.callbacks) this.callbacks.remove(name)
    }
}
