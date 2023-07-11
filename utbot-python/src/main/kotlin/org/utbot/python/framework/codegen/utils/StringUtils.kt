package org.utbot.python.framework.codegen.utils

import java.nio.file.FileSystems


fun String.toRelativeRawPath(): String {
    return "os.path.dirname(__file__) + r'${FileSystems.getDefault().separator}${this}'"
}