package org.bereft.simulation

import kotlinx.coroutines.*
import kotlinx.datetime.Instant
import org.bereft.core.*

/**
 * Drives the simulation loop.
 */
class SimulationController(
    private val assetKeys: List<AssetKey>,
    private val startTime: Instant,
    private val endTime: Instant,
    private val timeStepMillis: Long, // Time step in milliseconds
    private val worldStateAssembler: WorldStateAssembler,
    private val agent: IAgent,
    private val actionInterpreter: IActionInterpreter,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default // Allow injecting dispatcher
) {
    var currentTime: Instant = startTime
    private var simulationJob: Job? = null

    fun startSimulation() {
        if (simulationJob?.isActive == true) {
            println("Simulation already running.")
            return
        }
        println("Starting simulation from $startTime to $endTime with step ${timeStepMillis}ms")
        simulationJob = CoroutineScope(dispatcher).launch {
            while (isActive && currentTime <= endTime) {
                val currentStepTime = currentTime

                // Process each asset (sequentially or potentially in parallel)
                assetKeys.forEach { assetKey ->
                    try {
                        // 1. Assemble State
                        val stateVector = worldStateAssembler.assembleStateVector(assetKey, currentStepTime)

                        // 2. Agent Decision
                        val actionVector = agent.decideAction(stateVector)

                        // 3. Interpret & Execute Action
                        val context = SimulationContext(currentStepTime, assetKey)
                        actionInterpreter.interpretAndExecute(actionVector, context)

                    } catch (e: Exception) {
                        println("Error during simulation step for $assetKey at $currentStepTime: ${e.message}")
                        // Decide whether to continue or stop simulation on error
                        // cancel() // Example: Stop on error
                    }
                }

                // Advance time
                currentTime = Instant.fromEpochMilliseconds(currentStepTime.toEpochMilliseconds() + timeStepMillis)

                // Optional delay to simulate real-time or reduce CPU load
                // delay(10) // e.g., delay 10ms
            }
            println("Simulation finished at time $currentTime")
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        println("Simulation stopped.")
    }
}
