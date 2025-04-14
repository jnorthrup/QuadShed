package com.vsiwest.rangle.platform

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

actual object FileSystem {
    actual val pathSeparator: String = File.separator

    actual fun fileExists(path: String): Boolean = File(path).exists()

    actual fun createDirectories(path: String): Boolean = File(path).mkdirs()

    actual fun moveFile(source: String, target: String, overwrite: Boolean): Boolean {
        return try {
            val options = if (overwrite) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
            Files.move(Paths.get(source), Paths.get(target), *options)
            true
        } catch (e: Exception) {
            println("Error moving file: ${e.message}")
            false
        }
    }

    actual fun deleteFile(path: String): Boolean = File(path).delete()

    actual fun deleteDirectory(path: String): Boolean {
        val directory = File(path)
        if (!directory.exists()) return true // Already gone

        return directory.deleteRecursively()
    }

    actual fun appendText(path: String, text: String) {
        File(path).appendText(text)
    }

    actual fun readText(path: String): String = File(path).readText()

    actual fun findDirectories(basePath: String, maxDepth: Int, minDepth: Int): List<String> {
        val baseFile = File(basePath)
        if (!baseFile.exists() || !baseFile.isDirectory) return emptyList()

        val results = mutableListOf<String>()
        fun traverse(file: File, depth: Int) {
            if (depth > maxDepth) return
            if (depth >= minDepth && file.isDirectory) {
                results.add(file.absolutePath)
            }
            if (file.isDirectory) {
                file.listFiles()?.forEach {
                    traverse(it, depth + 1)
                }
            }
        }
        traverse(baseFile, 0)
        return results
    }

    actual fun getBasePath(path: String): String = File(path).parent

    actual fun getFileName(path: String): String = File(path).name
}
