package org.utbot.python.framework.codegen.utils


fun String.toPythonRawString(): String {
    return "r'${this}'"
}