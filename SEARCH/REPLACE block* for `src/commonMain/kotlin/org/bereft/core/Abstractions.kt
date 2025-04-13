package org.bereft.core

import kotlinx.datetime.Instant // Requires kotlinx-datetime dependency

// --- Core Data Structures (Conceptual Placeholders) ---

// Assuming Series<T> = Join<Int, (Int) -> T> exists from QuadShed/TrikeShed lib
// Replace with actual import
typealias Series<T> = borg.trikeshed.lib.Series<T>
typealias AssetKey = borg.trikeshed.model.AssetKey // Or a common definition
typealias TradeOrderAction = FloatArray // Agent's output (e.g., buy%, sell%, price_factor)

// --- Interfaces ---

/** Represents a source of time-series or state data */
interface IDataSourceReader<T : Any> {
    /** Gets the value nearest to a specific time (implementation detail: exact match, interpolation?) */
    suspend fun getCurrentValue(assetKey: AssetKey, timestamp: Instant): T?

    /** Gets a series of data within a time range */
    suspend fun getTimeSlice(assetKey: AssetKey, start: Instant, end: Instant): Series<T>?

    /** Gets the last N data points up to a given time */
    suspend fun getRollingWindow(assetKey: AssetKey, currentTime: Instant, lookbackPeriods: Int): Series<T>?
}

/** Configuration for a specific indicator instance */
data class IndicatorConfig(
    val type: String, // e.g., "BBands", "RSI"
    val parameters: Map<String, Any> // e.g., "windowSize" to 20, "k" to 2.0
)

/** Extracts derived features from input data */
interface IFeatureExtractor<In : Any, Out : Any> {
    /** Names of the output features this extractor produces */
    fun getFeatureNames(inputColumnNames: Series<String>, config: IndicatorConfig): Series<String>

    /** Calculates features over a whole series (batch/window) */
    suspend fun calculateFeatures(inputSeries: Series<In>, config: IndicatorConfig): Series<Out>?

    /** Updates internal state with new input and returns latest feature values (streaming) */
    suspend fun updateAndGetFeatures(newValue: In, config: IndicatorConfig): Out?

    /** Returns the minimum number of input periods needed before calculation can start */
    fun requiredLookback(config: IndicatorConfig): Int
}

/** The decision-making component */
interface IAgent {
    /** Receives the current world state vector and returns a vector of actions */
    suspend fun decideAction(stateVector: FloatArray): TradeOrderAction
}

/** Context passed to the action interpreter */
data class SimulationContext(
    val currentTime: Instant,
    val assetKey: AssetKey // The asset this action applies to (if action is per-asset)
    // Add other relevant context: current price, wallet state snapshot, etc.
)

/** Translates agent actions into concrete simulation effects */
interface IActionInterpreter {
    /** Takes the agent's action vector and applies it to the simulation state */
    suspend fun interpretAndExecute(actionVector: TradeOrderAction, context: SimulationContext)
}

// --- Concrete Data Example: OHLCV ---
// Often passed as input to extractors
data class OHLCV(
    val timestamp: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double // Use Double for consistency, Long might be better
)
