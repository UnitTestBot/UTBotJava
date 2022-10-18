package org.utbot.examples.codegen

// TODO: currently we can't properly handle properties in constructors, change CustomClass to data class after that is fixed
class CustomClass {
    var x: Int = 0
    var y: Int = 0

    fun f(): Int {
       return 0
    }
}

fun topLevelSum(a: Int, b: Int): Int {
    return a + b
}

fun Int.extensionOnBasicType(other: Int): Int {
    return this + other
}

fun CustomClass.extensionOnCustomClass(other: CustomClass): Boolean {
    return x >= other.x && y >= other.y
}