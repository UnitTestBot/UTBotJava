package org.utbot.framework.process.kryo

open class ProcessCommunicationException(msg: String?, cause: Throwable? = null) : Exception(msg, cause)

class ReadingFromKryoException(e: Throwable) :
    ProcessCommunicationException("Reading from Kryo exception |> ${e.stackTraceToString()}", e)

class WritingToKryoException(e: Throwable) :
    ProcessCommunicationException("Writing to Kryo exception |> ${e.stackTraceToString()}", e)