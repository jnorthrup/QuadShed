package com.vsiwest.rangle.binance.config

actual object KeyPairGeneratorWrapper {
    actual fun generateKeyPair(): Pair<String, String> {
        // Mock implementation for testing
        return "mockPrivateKey" to "mockPublicKey"
    }
}
