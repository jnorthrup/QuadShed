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
    val processBuilder = ProcessBuilder(*command.split(" ").toTypedArray())
    workingDir?.let { processBuilder.directory(java.io.File(it)) }
    environment.forEach { (key, value) -> processBuilder.environment()[key] = value }

    val process = processBuilder.start()
    process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)

    val stdout = mutableListOf<String>()
    val stderr = mutableListOf<String>()

    BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
        lines.forEach { stdout.add(it) }
    }
    BufferedReader(InputStreamReader(process.errorStream)).useLines { lines ->
        lines.forEach { stderr.add(it) }
    }

    val exitCode = process.exitValue() // This will throw if the process is still running
    return CommandResult(exitCode, stdout, stderr)
}
