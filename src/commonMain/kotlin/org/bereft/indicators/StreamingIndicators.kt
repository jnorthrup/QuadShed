package org.bereft.indicators

import org.bereft.core.IndicatorConfig
import org.bereft.core.OHLCV
import org.bereft.core.IFeatureExtractor
import org.bereft.core.AssetKey
import org.bereft.core.Series

class StreamingIndicatorFeatureExtractor : IFeatureExtractor<Double, DoubleArray> {

    private val indicatorStates = mutableMapOf<String, StreamingIndicator<*, *>>()

    private fun getIndicatorKey(assetKey: AssetKey, config: IndicatorConfig): String {
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
            when (config.type) {
                "BBands" -> BollingerBands(
                    windowSize = config.parameters["windowSize"] as? Int ?: 20,
                    k = config.parameters["k"] as? Double ?: 2.0
                )
                "RSI" -> Rsi(windowSize = config.parameters["windowSize"] as? Int ?: 14)
                "EMA" -> Ema(windowSize = config.parameters["windowSize"] as? Int ?: 12)
                else -> throw IllegalArgumentException("Unsupported indicator type: ${config.type}")
            }
        } as? StreamingIndicator<IN, OUT>
    }

    override fun getFeatureNames(inputColumnNames: Series<String>, config: IndicatorConfig): Series<String> {
        val baseName = inputColumnNames.getOrNull(0) ?: "value"
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
            else -> borg.trikeshed.lib.emptySeries()
        }
    }

    override suspend fun calculateFeatures(inputSeries: Series<Double>, config: IndicatorConfig): Series<DoubleArray>? {
        val tempIndicator = getOrCreateIndicator<Double, Any>(AssetKey("TEMP"), config)?.apply { reset() }
            ?: return null

        val results = mutableListOf<DoubleArray?>()
        inputSeries.forEach { value ->
            tempIndicator.update(value)
            results.add(flattenIndicatorOutput(tempIndicator.value))
        }
        return borg.trikeshed.lib.Series(results.size) { i -> results[i] ?: DoubleArray(getFeatureNames(borg.trikeshed.common.collections.s_[""], config).size) }
    }

    override suspend fun updateAndGetFeatures(newValue: Double, config: IndicatorConfig): DoubleArray? {
        val indicator = getOrCreateIndicator<Double, Any>(AssetKey("SINGLE_CONTEXT"), config)
            ?: return null

        indicator.update(newValue)
        return flattenIndicatorOutput(indicator.value)
    }

    override fun requiredLookback(config: IndicatorConfig): Int {
        val indicator = getOrCreateIndicator<Any, Any>(AssetKey("LOOKBACK_QUERY"), config)
        return indicator?.windowSize ?: 0
    }

    private fun flattenIndicatorOutput(output: Any?): DoubleArray? {
        return when (output) {
            is BollingerBandsValue -> doubleArrayOf(output.upperBand, output.middleBand, output.lowerBand, output.standardDeviation)
            is Double -> doubleArrayOf(output)
            is MacdValue -> doubleArrayOf(output.macdLine ?: Double.NaN, output.signalLine ?: Double.NaN, output.histogram ?: Double.NaN)
            null -> null
            else -> null
        }
    }
}
