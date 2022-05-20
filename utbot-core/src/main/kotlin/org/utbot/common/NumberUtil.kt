package org.utbot.common

fun Any?.isDefaultValue(): Boolean {
    return this == null || (this is Number && this.toDouble() == 0.0) || (this is Char && this == '\u0000')
}