package com.ccg.slrcore.common

/**
 * Unit used by a filter. Can be used to chain filters together.
 * @version 1.0.0
 */
class FilterUnit <T> (val mapping: List<T>, val probabilities: FloatArray)

/**
 * Argmax will return the index of the largest element in an array
 * @link https://stackoverflow.com/a/65406319
 * @version 1.0.0
 */
fun <T : Comparable<T>> Iterable<T>.argmax(): Int {
    return withIndex().maxBy { it.value }.index
}

/**
 * Argmax implemented on a float array
 * @author Ananay Vikram Gupta
 * @version 1.0.0
 */
fun FloatArray.argmax(): Int {
    return toList().argmax()
}
fun interface PredictionFilter<T> {
    fun filter(input: FilterUnit<T>): FilterUnit<T>
}

class PassThroughFilterSingle<T>: PredictionFilter<T> {
    override fun filter(input: FilterUnit<T>): FilterUnit<T> {
        if (input.mapping.size != input.probabilities.size) throw IllegalArgumentException("Received Invalid Mapping and Prediction pair.")
        if (input.mapping.isEmpty())  return input
        return FilterUnit(listOf(input.mapping[input.probabilities.argmax()]), arrayOf(input.probabilities.max()).toFloatArray())
    }
}

class Thresholder<T>(private val threshold: Float):
    PredictionFilter<T> {
    override fun filter(input: FilterUnit<T>): FilterUnit<T> {
        if (input.mapping.size != input.probabilities.size) throw IllegalArgumentException("Received Invalid Mapping and Prediction pair.")
        if (input.mapping.isEmpty())  return input
        return FilterUnit(
            input.mapping.filterIndexed { idx, _ -> input.probabilities[idx] > threshold},
            input.probabilities.filter { value -> value > threshold }.toFloatArray()
        )
    }
}

class FocusSublistFilter<T>(private val focusSublist: List<T>): PredictionFilter<T> {
    override fun filter(input: FilterUnit<T>): FilterUnit<T> {
        if (input.mapping.size != input.probabilities.size) throw IllegalArgumentException("Received Invalid Mapping and Prediction pair.")
        if (input.mapping.isEmpty())  return input

        val indices = input.mapping
            .mapIndexed { idx, value -> if (value in focusSublist) idx else -1 }
            .filter { it != -1 }

        return FilterUnit(
            input.mapping.filterIndexed { idx, _ -> idx in indices },
            input.probabilities.filterIndexed { idx, _ -> idx in indices}.toFloatArray()
        )
    }
}