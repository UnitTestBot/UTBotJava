package org.utbot.instrumentation.util

import org.utbot.framework.plugin.api.InstrumentedProcessDeathException

// TODO: refactor this

/**
 * Base class for instrumentation exceptions.
 */
open class InstrumentationException(msg: String?, cause: Throwable? = null) : Exception(msg, cause)

abstract class NoFieldException(clazz: Class<*>, name: String, fieldName: String) :
    InstrumentationException(
        buildString {
            appendLine("No $name found")
            appendLine("\tClazz: $clazz")
            appendLine("\t${name.capitalize()} name: $fieldName")
            append("All fields: ${clazz.declaredFields.joinToString { it.name }}")
        }
    )

class NoProbesArrayException(clazz: Class<*>, arrayName: String) :
    NoFieldException(clazz, "probes array", arrayName)

class NoProbeCounterException(clazz: Class<*>, counterName: String) :
    NoFieldException(clazz, "probe counter", counterName)

class CastProbesArrayException :
    InstrumentationException("Can't cast probes array to Boolean array")

class CastProbeCounterException :
    InstrumentationException("Can't cast probe counter to Int")

/**
 * this exception is thrown only in main process.
 * currently it means that {e: Throwable} happened in instrumented process,
 * but instrumented process still can operate and not dead.
 * on instrumented process death - [InstrumentedProcessDeathException] is thrown
 */
class InstrumentedProcessError(e: Throwable) :
    InstrumentationException("Error in the instrumented process |> ${e.stackTraceToString()}", e)