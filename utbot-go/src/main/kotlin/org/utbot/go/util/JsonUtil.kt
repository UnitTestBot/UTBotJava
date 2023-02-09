package org.utbot.go.util

import com.beust.klaxon.Klaxon
import java.io.File

fun <T> convertObjectToJsonString(targetObject: T): String = Klaxon().toJsonString(targetObject)

fun <T> writeJsonToFileOrFail(targetObject: T, jsonFile: File) {
    val targetObjectAsJson = convertObjectToJsonString(targetObject)
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