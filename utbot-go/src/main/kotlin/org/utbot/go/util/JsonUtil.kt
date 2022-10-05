package org.utbot.go.util

import com.beust.klaxon.Klaxon
import java.io.File

fun <T> writeJsonToFileOrFail(targetObject: T, jsonFile: File) {
    val targetObjectAsJson = Klaxon().toJsonString(targetObject)
    jsonFile.writeText(targetObjectAsJson)
}

inline fun <reified T> parseFromJsonOrFail(jsonFile: File): T {
    val result = Klaxon().parse<T>(jsonFile)
    if (result == null) {
        val rawResults = try {
            jsonFile.readText()
        } catch (exception: Exception) {
            null
        }
        throw RuntimeException(
            "Failed to deserialize results: $rawResults"
        )
    }
    return result
}