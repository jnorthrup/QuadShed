package org.bereft.core

import kotlinx.datetime.Instant

typealias AssetKey = borg.trikeshed.model.AssetKey
typealias TradeOrderAction = FloatArray

interface IDataSourceReader<T : Any> {
    suspend fun getCurrentValue(assetKey: AssetKey, timestamp: Instant): T?
    suspend fun getTimeSlice(assetKey: AssetKey, start: Instant, end: Instant): Series<T>?
    suspend fun getRollingWindow(assetKey: AssetKey, currentTime: Instant, lookbackPeriods: Int): Series<T>?
}

data class IndicatorConfig(
    val type: String,
    val parameters: Map<String, Any>
)

interface IFeatureExtractor<In : Any, Out : Any> {
    fun getFeatureNames(inputColumnNames: Series<String>, config: IndicatorConfig): Series<String>
    suspend fun calculateFeatures(inputSeries: Series<In>, config: IndicatorConfig): Series<Out>?
    suspend fun updateAndGetFeatures(newValue: In, config: IndicatorConfig): Out?
    fun requiredLookback(config: IndicatorConfig): Int
}

interface IAgent {
    suspend fun decideAction(stateVector: FloatArray): TradeOrderAction
}

data class SimulationContext(
    val currentTime: Instant,
    val assetKey: AssetKey
)

interface IActionInterpreter {
    suspend fun interpretAndExecute(actionVector: TradeOrderAction, context: SimulationContext)
}

data class OHLCV(
    val timestamp: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)
