package org.bereft.indicators

import org.bereft.core.IndicatorConfig
import org.bereft.core.OHLCV
// Assume StreamingIndicator, BollingerBandsValue, SmaStdDevCalculator, BollingerBands
// are defined here as in the previous example, potentially using a KMP CircularQueue.

// --- Example Feature Extractor Implementation ---

class StreamingIndicatorFeatureExtractor : IFeatureExtractor<Double, DoubleArray> { // Output multiple values

    // Internal map to hold stateful indicators per assetKey/config combo
    // Key could be String like "${assetKey.symbol}_BBands_20_2.0"
    private val indicatorStates = mutableMapOf<String, StreamingIndicator<*, *>>()

    private fun getIndicatorKey(assetKey: AssetKey, config: IndicatorConfig): String {
        // Create a unique key based on asset and config parameters
        return "${assetKey.symbol}_${config.type}_" +
               config.parameters.entries.sortedBy { it.key }.joinToString("_") { "${it.key}${it.value}" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <IN, OUT> getOrCreateIndicator(
        assetKey: AssetKey,
        config: IndicatorConfig
    ): StreamingIndicator<IN, OUT>? {
        val key = getIndicatorKey(assetKey, config)
        return indicatorStates.getOrPut(key) {
            // Factory logic to create the correct indicator based on config.type
            when (config.type) {
                "BBands" -> BollingerBands(
                    windowSize = config.parameters["windowSize"] as? Int ?: 20,
                    k = config.parameters["k"] as? Double ?: 2.0
                )
                "RSI" -> Rsi(windowSize = config.parameters["windowSize"] as? Int ?: 14)
                "EMA" -> Ema(windowSize = config.parameters["windowSize"] as? Int ?: 12)
                // Add other indicators...
                else -> throw IllegalArgumentException("Unsupported indicator type: ${config.type}")
            }
        } as? StreamingIndicator<IN, OUT> // Unsafe cast, relies on correct IN type mapping
    }

    override fun getFeatureNames(inputColumnNames: Series<String>, config: IndicatorConfig): Series<String> {
        val baseName = inputColumnNames.getOrNull(0) ?: "value" // Use first input name as base
        val params = config.parameters.entries.sortedBy { it.key }.joinToString("") { "${it.value}" }
        return when (config.type) {
            "BBands" -> borg.trikeshed.common.collections.s_[
                "${baseName}_BB_Upper_${params}",
                "${baseName}_BB_Middle_${params}",
                "${baseName}_BB_Lower_${params}",
                "${baseName}_BB_StdDev_${params}"
            ]
            "RSI" -> borg.trikeshed.common.collections.s_["${baseName}_RSI_${params}"]
            "EMA" -> borg.trikeshed.common.collections.s_["${baseName}_EMA_${params}"]
            // Add other indicators...
            else -> borg.trikeshed.lib.emptySeries()
        }
    }

    // Batch calculation - Less ideal for streaming indicators, might re-implement naively
    override suspend fun calculateFeatures(inputSeries: Series<Double>, config: IndicatorConfig): Series<DoubleArray>? {
        // Create a temporary indicator, feed all data, collect results
        // This is inefficient compared to streaming updates but fulfills the interface contract.
        val tempIndicator = getOrCreateIndicator<Double, Any>(AssetKey("TEMP"), config)?.apply { reset() } // Use dummy AssetKey
            ?: return null

        val results = mutableListOf<DoubleArray?>()
        inputSeries.forEach { value ->
            tempIndicator.update(value)
            results.add(flattenIndicatorOutput(tempIndicator.value))
        }
        return borg.trikeshed.lib.Series(results.size) { i -> results[i] ?: DoubleArray(getFeatureNames(borg.trikeshed.common.collections.s_[""], config).size) } // Default on null
    }


    override suspend fun updateAndGetFeatures(newValue: Double, config: IndicatorConfig): DoubleArray? {
       // This assumes the caller manages separate indicator instances per asset key
       // A better design might pass the asset key here too.
       // For simplicity, let's assume a single implicit context for update.
       val indicator = getOrCreateIndicator<Double, Any>(AssetKey("SINGLE_CONTEXT"), config)
           ?: return null // Or throw?

       indicator.update(newValue)
       return flattenIndicatorOutput(indicator.value)
    }

     override fun requiredLookback(config: IndicatorConfig): Int {
         // Get lookback from a temporary instance
         val indicator = getOrCreateIndicator<Any, Any>(AssetKey("LOOKBACK_QUERY"), config)
         return indicator?.windowSize ?: 0
     }


    // Helper to convert various indicator outputs to a consistent DoubleArray
    private fun flattenIndicatorOutput(output: Any?): DoubleArray? {
        return when (output) {
            is BollingerBandsValue -> doubleArrayOf(output.upperBand, output.middleBand, output.lowerBand, output.standardDeviation)
            is Double -> doubleArrayOf(output) // For RSI, EMA
            is MacdValue -> doubleArrayOf(output.macdLine ?: Double.NaN, output.signalLine ?: Double.NaN, output.histogram ?: Double.NaN)
            null -> null
            else -> null // Unknown indicator output type
        }
    }
}
