package org.utbot.examples.objects

import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction3
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

internal class ObjectWithPrimitivesClassTest : UtValueTestCaseChecker(testClass = ObjectWithPrimitivesClass::class) {
    @Test
    fun testDefaultConstructor() {
        val method: KFunction0<ObjectWithPrimitivesClass> = ::ObjectWithPrimitivesClass
        checkStaticMethod(
            method,
            eq(1),
            // TODO: SAT-933 Add support for constructor testing")
            // { instance -> instance is ObjectWithPrimitivesClass },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testConstructorWithParams() {
        val method: KFunction3<Int, Int, Double, ObjectWithPrimitivesClass> = ::ObjectWithPrimitivesClass
        checkStaticMethod(
            method,
            eq(1),
            // TODO: SAT-933 Add support for constructor testing")
//            { x, y, weight, instance ->
//                instance is ObjectWithPrimitivesClass && instance.x == x && instance.y == y && instance.weight == weight
//            },
            coverage = DoNotCalculate
        )
    }
}
