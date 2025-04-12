package com.vsiwest.rangle.indicators

import borg.trikeshed.common.collections.CircularQueue

/**
 * Streaming implementation of Simple Moving Average (SMA).
 */
class Sma(
    override val windowSize: Int
) : StreamingIndicator<Double, Double> {

    init {
        require(windowSize > 0) { "Window size must be positive" }
    }

    private val window = CircularQueue<Double>(windowSize)
    private var sum: Double = 0.0
    private var _currentValue: Double? = null

    override val value: Double? get() = _currentValue

    override fun isReady(): Boolean = window.isFull()

    override fun update(input: Double) {
        if (window.isFull()) {
            sum -= window.poll()
        }
        window.offer(input)
        sum += input
        _currentValue = sum / window.size
    }

    override fun reset() {
        window.clear()
        sum = 0.0
        _currentValue = null
    }
}
