package org.utbot.python.framework.codegen


fun String.toPythonRawString(): String {
    return "r'${this}'"
}