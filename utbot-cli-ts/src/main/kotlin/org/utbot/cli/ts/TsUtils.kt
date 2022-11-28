package org.utbot.cli.ts

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.sources.ValueSource
import org.json.JSONObject
import java.io.File

internal object TsUtils {

    fun makeAbsolutePath(path: String): String {
//        println("I got \"$path\", user.dir is ${System.getProperty("user.dir")}")
        var rawPath = when {
            File(path).isAbsolute -> ""
            else -> System.getProperty("user.dir") + if (path != "") "/" else ""
        }
        rawPath += path
        return rawPath.replace("\\", "/")
    }
}

class JsonValueSource(
    private val root: JSONObject,
) : ValueSource {
    override fun getValues(context: Context, option: Option): List<ValueSource.Invocation> {
        val cursor: String
        val part = ValueSource.name(option)
        try {
            cursor = root.getString(part)
        } catch (e: Exception) {
            return emptyList()
        }
        return ValueSource.Invocation.just(cursor)
    }

    companion object {
        fun from(file: String): JsonValueSource {
            val json = JSONObject(file)
            return JsonValueSource(json)
        }

    }
}
