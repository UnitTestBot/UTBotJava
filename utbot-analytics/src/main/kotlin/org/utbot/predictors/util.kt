package org.utbot.predictors

fun String.splitByCommaIntoDoubleArray() =
    try {
        split(',').map(String::toDouble).toDoubleArray()
    } catch (e: NumberFormatException) {
        error("Wrong format in $this, expect doubles separated by commas")
    }