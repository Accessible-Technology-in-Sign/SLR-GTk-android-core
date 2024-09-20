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


abstract class TfLiteModelManager(
        model: File,
    ) {
    protected val interpreter: Interpreter = Interpreter(
        model,
        Interpreter.Options().apply{ this.setNumThreads(Config.NUM_WORKER_THREADS_MEDIAPIPE) }
    )
}

class SLRTfLiteModel <T> (
    model: File,
    val mapping: List<T>
): TfLiteModelManager(model) {
    private val modelInputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 60, 21 * 2, 1), DataType.FLOAT32)
    private val modelOutputTensor = TensorBuffer.createFixedSize(interpreter.getOutputTensor(0).shape(), DataType.FLOAT32)

    var outputFilters: MutableList<PredictionFilter<T>> = LinkedList<PredictionFilter<T>>().also { it.add(PassThroughFilterSingle()) }

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
    fun addCallback(name: String, callback: SLREventHandler<T>) {
        this.callbacks[name] = callback
    }

    fun removeCallback(name: String) {
        if (name in this.callbacks) this.callbacks.remove(name)
    }
}
