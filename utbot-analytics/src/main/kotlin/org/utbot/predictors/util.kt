package org.utbot.predictors

fun String.splitByCommaIntoDoubleArray() =
    split(',').map(String::toDouble).toDoubleArray()