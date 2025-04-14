package com.vsiwest.rangle.platform

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

actual fun executeCommand(
    command: String,
    workingDir: String?,
    environment: Map<String, String>,
    timeoutMillis: Long
): CommandResult {
    val processBuilder = ProcessBuilder(*splitCommand(command).toTypedArray())
    workingDir?.let { processBuilder.directory(java.io.File(it)) }
    val environmentMap = processBuilder.environment()
    environmentMap.clear() // Clear existing environment
    environmentMap.putAll(environment) // Add provided environment

    val process = processBuilder.start()

    val stdoutLines = mutableListOf<String>()
    val stderrLines = mutableListOf<String>()

    val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
    val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

    val completed = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)

    if (!completed) {
        process.destroyForcibly() // Ensure process is killed
        return CommandResult(
            exitCode = -1, // Indicate timeout
            stdout = emptyList(),
            stderr = listOf("Command timed out after ${timeoutMillis}ms")
        )
    }

    stdoutReader.useLines { lines ->
        lines.forEach { stdoutLines.add(it) }
    }

    stderrReader.useLines { lines ->
        lines.forEach { stderrLines.add(it) }
    }

    val exitCode = process.exitValue()

    return CommandResult(
        exitCode = exitCode,
        stdout = stdoutLines,
        stderr = stderrLines
    )
}

// Helper function to split the command string into a list of arguments
private fun splitCommand(command: String): List<String> {
    val parts = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes: Char? = null

    for (char in command) {
        when {
            inQuotes == null && (char == '"' || char == '\'') -> {
                inQuotes = char
            }
            inQuotes == char -> {
                inQuotes = null
            }
            inQuotes == null && char.isWhitespace() -> {
                if (current.isNotEmpty()) {
                    parts.add(current.toString())
                    current = StringBuilder()
                }
            }
            else -> {
                current.append(char)
            }
        }
    }

    if (current.isNotEmpty()) {
        parts.add(current.toString())
    }

    return parts
}
