package org.utbot.common

import java.io.OutputStream
import java.io.PrintStream

fun silentlyCloseStandardStreams() {
    // we should change out/err streams as not to spend time on user output
    // and also because rd default logging system writes some initial values to stdout, polluting it as well
    val tmpStream = PrintStream(object : OutputStream() {
        override fun write(b: Int) {}
    })
    val prevOut = System.out
    val prevError = System.err
    System.setOut(tmpStream)
    System.setErr(tmpStream)
    // stdin/stderr should be closed as not to leave hanging descriptors
    // and we cannot log any exceptions here as rd remote logging is still not configured
    // so we pass any exceptions
    silent { prevOut.close() }
    silent { prevError.close() }
}