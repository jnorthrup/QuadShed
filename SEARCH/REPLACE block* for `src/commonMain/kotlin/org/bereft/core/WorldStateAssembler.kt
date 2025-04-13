package org.bereft.core

import borg.trikeshed.lib.* // Series, Join, j, combine, etc.
import borg.trikeshed.common.collections.s_ // For s_
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant
import kotlin.math.max

/**
 * Responsible for gathering data, applying feature extractors,
 * and assembling the final numerical state vector for a given asset key and time.
 */
class WorldStateAssembler(
    // --- Dependencies (Injected) ---
    private val ohlcvReader: IDataSourceReader<OHLCV>, // Specific reader for candles
    private val walletReader: IDataSourceReader<Map<String, Double>>, // Reader for wallet state (Asset -> Amount)
    private val featureExtractors: List<Pair<IFeatureExtractor<*, *>, List<IndicatorConfig>>>, // Extractor + its configs
    // --- Configuration ---
    private val stateVectorLayout: List<String>, // Defines the exact order and content of the output vector
    private val globalLookback: Int = 200 // Max lookback needed across all features
) {

    // Pre-calculate the total size of the state vector based on layout
    // Requires feature extractors to reliably report their output size via getFeatureNames
    val stateVectorSize: Int by lazy {
        // This is complex - needs to sum sizes based on stateVectorLayout definition
        // Placeholder calculation:
        stateVectorLayout.size // Needs proper calculation based on features included
    }

    suspend fun assembleStateVector(assetKey: AssetKey, currentTime: Instant): FloatArray {
        return coroutineScope {
            // 1. Fetch Raw Data Asynchronously
            val ohlcvDataDeferred = async {
                ohlcvReader.getRollingWindow(assetKey, currentTime, globalLookback)
                    ?: s_<OHLCV>() // Empty series if no data
            }
            val walletDataDeferred = async {
                walletReader.getCurrentValue(assetKey, currentTime) // Assuming wallet state is point-in-time
                    ?: emptyMap()
            }

            // Wait for raw data
            val ohlcvHistory = ohlcvDataDeferred.await()
            val currentWallet = walletDataDeferred.await()

            // Check if we have enough base data
            if (ohlcvHistory.size < globalLookback) {
                // Not enough history, return zeros or specific padding
                println("Warning: Not enough history for ${assetKey.symbol} at $currentTime (${ohlcvHistory.size}/$globalLookback)")
                return@coroutineScope FloatArray(stateVectorSize) { 0.0f } // Padded vector
            }

            // 2. Extract Base OHLCV Features (e.g., latest close, high, low, volume)
            // We need the *most recent* candle from the history for current state
            val latestCandle = ohlcvHistory.lastOrNull()
                ?: return@coroutineScope FloatArray(stateVectorSize) { 0.0f } // Handle empty history case


            // 3. Calculate Derived Features (Indicators)
            val featureResults = mutableMapOf<String, FloatArray>() // Store results keyed by feature name base

            for ((extractor, configs) in featureExtractors) {
                 for (config in configs) {
                     // Determine input data series based on config (e.g., close prices)
                      // This logic needs to be more robust - mapping config to input columns
                     val inputSeries = if (extractor is StreamingIndicatorFeatureExtractor) { // Example check
                         ohlcvHistory.map { it.close } // Assuming close price input
                     } else {
                         // Handle other extractor input types if needed
                         s_() // Empty series if input type unknown
                     }

                     if (inputSeries.isNotEmpty()) {
                         // Calculate features using batch method for simplicity here
                         // A streaming approach would update internal state instead
                         val calculatedFeaturesSeries = extractor.calculateFeatures(inputSeries as Series<Any>, config)

                         // Get the *latest* calculated feature values
                         val latestFeatures = calculatedFeaturesSeries?.lastOrNull()

                         if (latestFeatures != null) {
                             // Store based on *generated* feature names from extractor
                             val featureNames = extractor.getFeatureNames(s_["close"], config) // Assume close input
                             if (featureNames.size == latestFeatures.size) {
                                 for (i in 0 until featureNames.size) {
                                     featureResults[featureNames[i]] = floatArrayOf(latestFeatures[i].toFloat()) // Store as FloatArray for consistency
                                 }
                             } else {
                                  println("Warning: Feature name/value count mismatch for ${config.type}")
                             }
                         }
                     }
                 }
            }


            // 4. Assemble Final State Vector based on `stateVectorLayout`
            val finalVector = FloatArray(stateVectorSize)
            var currentIndex = 0

            fun addValues(values: FloatArray?) {
                 if (values != null) {
                    for (value in values) {
                         if (currentIndex < stateVectorSize) {
                             finalVector[currentIndex++] = value.takeUnless { it.isNaN() } ?: 0.0f // Handle NaN
                         }
                    }
                 } else {
                     // Handle case where feature calculation failed or wasn't ready
                     // Increment index appropriately based on expected size (difficult!)
                      println("Warning: Null feature result encountered during assembly")
                      // Need a way to know how many slots to skip or fill with padding
                 }
            }

             // Example assembly - This needs to precisely match `stateVectorLayout`
             addValues(floatArrayOf(latestCandle.close.toFloat())) // Example: Add latest close
             addValues(featureResults["close_BB_Upper_..."]) // Example: Add specific Bollinger band value
             addValues(featureResults["close_RSI_..."])     // Example: Add specific RSI value
             addValues(floatArrayOf(currentWallet[assetKey.tradeAsset]?.toFloat() ?: 0.0f)) // Example: Add wallet balance

            // TODO: Implement the full assembly loop based on stateVectorLayout


            if (currentIndex != stateVectorSize) {
                 println("Warning: Final vector size mismatch. Expected $stateVectorSize, got $currentIndex")
                 // Fill remaining with padding?
            }

            return@coroutineScope finalVector
        }
    }
}

// Placeholder for data needed by ActionInterpreter
data class ActionContext(val timestamp: Instant /* ... other needed data ... */)
