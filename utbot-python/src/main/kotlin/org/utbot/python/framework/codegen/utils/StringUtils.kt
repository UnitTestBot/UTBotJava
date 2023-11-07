package org.utbot.python.framework.codegen.utils

import java.nio.file.FileSystems


fun String.toRelativeRawPath(): String {
    val dirname = "os.path.dirname(__file__)"
    if (this.isEmpty()) {
        return dirname
    }
    return "$dirname + r'${FileSystems.getDefault().separator}${this}'"
}