package com.ccg.slrcore.common

/**
 * Represents a unit used by a filter, allowing for chaining of filters.
 *
 * @param <T> The type of the elements in the mapping list.
 * @param mapping List of mappings associated with the probabilities.
 * @param probabilities Array of probabilities corresponding to the mappings.
 * @version 1.0.0
 *
 */
class FilterUnit <T> (val mapping: List<T>, val probabilities: FloatArray)

/**
 * Extension function to compute the index of the largest element in an iterable.
 *
 * @param <T> The type of the elements, which must be comparable.
 * @return The index of the largest element.
 * @version 1.0.0
 * @link https://stackoverflow.com/a/65406319
 */
fun <T : Comparable<T>> Iterable<T>.argmax(): Int {
    return withIndex().maxBy { it.value }.index
}

/**
 * Extension function to compute the index of the largest element in a float array.
 *
 * @return The index of the largest element in the array.
 * @version 1.0.0
 */
fun FloatArray.argmax(): Int {
    return toList().argmax()
}


/**
 * Functional interface for filtering predictions.
 *
 * @param <T> The type of the elements being filtered.
 */
fun interface PredictionFilter<T> {
    /**
     * Applies a filter to the input FilterUnit.
     *
     * @param input The input FilterUnit to be filtered.
     * @return The filtered FilterUnit.
     */
    fun filter(input: FilterUnit<T>): FilterUnit<T>
}

/**
 * A filter that passes through the single highest-probability mapping.
 *
 * @param <T> The type of the elements in the mapping.
 */
class PassThroughFilterSingle<T>: PredictionFilter<T> {
    override fun filter(input: FilterUnit<T>): FilterUnit<T> {
        if (input.mapping.size != input.probabilities.size) throw IllegalArgumentException("Received Invalid Mapping and Prediction pair.")
        if (input.mapping.isEmpty())  return input
        return FilterUnit(listOf(input.mapping[input.probabilities.argmax()]), arrayOf(input.probabilities.max()).toFloatArray())
    }
}

/**
 * A filter that thresholds probabilities, retaining only mappings with probabilities above a given threshold.
 *
 * @param <T> The type of the elements in the mapping.
 */
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

/**
 * A filter that focuses on a predefined sublist of mappings.
 *
 * @param <T> The type of the elements in the mapping.
 */
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