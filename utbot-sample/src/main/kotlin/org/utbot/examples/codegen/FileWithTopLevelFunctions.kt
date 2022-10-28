package org.utbot.examples.codegen

class CustomClass

fun topLevelSum(a: Int, b: Int): Int {
    return a + b
}

fun Int.extensionOnBasicType(other: Int): Int {
    return this + other
}

fun CustomClass.extensionOnCustomClass(other: CustomClass): Boolean {
    return this === other
}