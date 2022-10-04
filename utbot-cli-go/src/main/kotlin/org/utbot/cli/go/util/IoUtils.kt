package org.utbot.cli.go.util

import java.io.InputStream
import java.io.OutputStream

fun copy(from: InputStream, to: OutputStream?) {
    val buffer = ByteArray(10240)
    var len: Int
    while (from.read(buffer).also { len = it } != -1) {
        to?.write(buffer, 0, len)
    }
}