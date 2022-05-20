package org.utbot.instrumentation.util

// TODO: refactor this

/**
 * Base class for instrumentation exceptions.
 */
open class InstrumentationException(msg: String?, cause: Throwable? = null) : Exception(msg, cause)

class NoProbesArrayException(clazz: Class<*>, arrayName: String) :
    InstrumentationException(
        "No probes array found\n\t" +
                "Clazz: $clazz\n\t" +
                "Probes array name: $arrayName\n\t" +
                "All fields: ${clazz.fields.joinToString { it.name }}"
    )

class CastProbesArrayException :
    InstrumentationException("Can't cast probes array to Boolean array")

class ReadingFromKryoException(e: Throwable) :
    InstrumentationException("Reading from Kryo exception |> ${e.stackTraceToString()}", e)

class WritingToKryoException(e: Throwable) :
    InstrumentationException("Writing to Kryo exception |> ${e.stackTraceToString()}", e)

class ChildProcessError(e: Throwable) :
    InstrumentationException("Error in the child process |> ${e.stackTraceToString()}", e)

class UnexpectedCommand(cmd: Protocol.Command) :
    InstrumentationException("Got unexpected command: $cmd")
