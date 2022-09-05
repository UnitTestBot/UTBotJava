package org.utbot.examples.casts

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation SAT-1332
internal class InstanceOfExampleTest : UtValueTestCaseChecker(
    testClass = InstanceOfExample::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testSimpleInstanceOf() {
        check(
            InstanceOfExample::simpleInstanceOf,
            eq(2),
            { o, r -> o is CastClassFirstSucc && r is CastClassFirstSucc },
            { o, r -> o !is CastClassFirstSucc && r == null },
        )
    }

    @Test
    fun testNullPointerCheck() {
        check(
            InstanceOfExample::nullPointerCheck,
            eq(3),
            { o, _ -> o == null },
            { o, r -> o is CastClassFirstSucc && r == o.z },
            { o, r -> o !is CastClassFirstSucc && o != null && r == o.x },
        )
    }

    @Test
    fun testVirtualCall() {
        check(
            InstanceOfExample::virtualCall,
            eq(2),
            { o, r -> o is CastClassFirstSucc && r == o.foo() },
            { o, r -> o !is CastClassFirstSucc && r == -1 },
        )
    }

    @Test
    fun testVirtualFunctionCallWithCast() {
        check(
            InstanceOfExample::virtualFunctionCallWithCast,
            eq(3),
            { o, r -> o !is CastClassFirstSucc && r == -1 },
            { o, _ -> o is CastClass && o !is CastClassFirstSucc },
            { o, r -> o is CastClassFirstSucc && r == o.z },
        )
    }

    @Test
    fun testVirtualCallWithoutOneInheritor() {
        check(
            InstanceOfExample::virtualCallWithoutOneInheritor,
            eq(4),
            { o, r -> o !is CastClassFirstSucc && o is CastClass && r == o.foo() },
            { o, r -> o is CastClassSecondSucc && r == o.foo() },
            { o, _ -> o == null },
            { o, r -> o is CastClassFirstSucc && r == o.foo() },
        )
    }

    @Test
    fun testVirtualCallWithoutOneInheritorInverse() {
        check(
            InstanceOfExample::virtualCallWithoutOneInheritorInverse,
            eq(4),
            { o, r -> o !is CastClassFirstSucc && o is CastClass && r == o.foo() },
            { o, r -> o is CastClassSecondSucc && r == o.foo() },
            { o, _ -> o == null },
            { o, r -> o is CastClassFirstSucc && r == o.foo() },
        )
    }

    @Test
    fun testWithoutOneInheritorOnArray() {
        check(
            InstanceOfExample::withoutOneInheritorOnArray,
            eq(2),
            { o, r -> o.isInstanceOfArray<CastClassFirstSucc>() && r == 0 },
            { o, r -> !o.isInstanceOfArray<CastClassFirstSucc>() && r == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testWithoutOneInheritorOnArrayInverse() {
        check(
            InstanceOfExample::withoutOneInheritorOnArrayInverse,
            eq(2),
            { o, r -> !o.isInstanceOfArray<CastClassFirstSucc>() && r == 0 },
            { o, r -> o.isInstanceOfArray<CastClassFirstSucc>() && r == 1 },
            coverage = DoNotCalculate
        )
    }


    @Test
    fun testInstanceOfAsPartOfInternalExpressions() {
        check(
            InstanceOfExample::instanceOfAsPartOfInternalExpressions,
            branches = ignoreExecutionsNumber,
            { o, r ->
                val o0isFirst = o[0].isInstanceOfArray<CastClassFirstSucc>()
                val o1isSecond = o[1].isInstanceOfArray<CastClassSecondSucc>()
                val and = o0isFirst && o1isSecond
                and && r == 1
            },
            { o, r ->
                val o0isSecond = o[0].isInstanceOfArray<CastClassSecondSucc>()
                val o1isFirst = o[1].isInstanceOfArray<CastClassFirstSucc>()
                val or = o0isSecond || o1isFirst
                or && r == 2
            },
            { o, r ->
                val o0isFirst = o[0].isInstanceOfArray<CastClassFirstSucc>()
                val o1isSecond = o[1].isInstanceOfArray<CastClassSecondSucc>()

                val o0isSecond = o[0].isInstanceOfArray<CastClassSecondSucc>()
                val o1isFirst = o[1].isInstanceOfArray<CastClassFirstSucc>()

                val and = o0isFirst && o1isSecond
                val or = o0isSecond || o1isFirst

                !and && !or && r == 3
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testInstanceOfAsPartOfInternalExpressionsCastClass() {
        check(
            InstanceOfExample::instanceOfAsPartOfInternalExpressionsCastClass,
            branches = ignoreExecutionsNumber,
            { o, r ->
                val o0isFirst = o[0].isInstanceOfArray<CastClass>()
                val o1isSecond = o[1].isInstanceOfArray<CastClass>()
                val and = o0isFirst && o1isSecond
                !and && r == 1
            },
            { o, r ->
                val o0isSecond = o[0].isInstanceOfArray<CastClassSecondSucc>()
                val o1isFirst = o[1].isInstanceOfArray<CastClassFirstSucc>()
                val or = o0isSecond || o1isFirst
                !or && r == 2
            },
            { o, r ->
                val o0isFirst = o[0].isInstanceOfArray<CastClass>()
                val o1isSecond = o[1].isInstanceOfArray<CastClass>()

                val o0isSecond = o[0].isInstanceOfArray<CastClassSecondSucc>()
                val o1isFirst = o[1].isInstanceOfArray<CastClassFirstSucc>()

                val and = o0isFirst && o1isSecond
                val or = o0isSecond || o1isFirst

                and && or && r == 3
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testInstanceOfAsPartOfInternalExpressionsXor() {
        check(
            InstanceOfExample::instanceOfAsPartOfInternalExpressionsXor,
            eq(4),
            { o, r ->
                val o0isSecond = o[0].isInstanceOfArray<CastClassSecondSucc>()
                val o1isFirst = o[1].isInstanceOfArray<CastClassFirstSucc>()
                r == 1 && !o0isSecond && o1isFirst
            },
            { o, r ->
                val o0isSecond = o[0].isInstanceOfArray<CastClassSecondSucc>()
                val o1isFirst = o[1].isInstanceOfArray<CastClassFirstSucc>()
                r == 2 && o0isSecond && !o1isFirst
            },
            { o, r ->
                val o0isSecond = o[0].isInstanceOfArray<CastClassSecondSucc>()
                val o1isFirst = o[1].isInstanceOfArray<CastClassFirstSucc>()
                r == 3 && o0isSecond && o1isFirst
            },
            { o, r ->
                val o0isSecond = o[0].isInstanceOfArray<CastClassSecondSucc>()
                val o1isFirst = o[1].isInstanceOfArray<CastClassFirstSucc>()
                r == 4 && !o0isSecond && !o1isFirst
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testInstanceOfAsPartOfInternalExpressionsXorInverse() {
        check(
            InstanceOfExample::instanceOfAsPartOfInternalExpressionsXorInverse,
            eq(4),
            { o, r ->
                val o0isSecond = o[0].isInstanceOfArray<CastClassSecondSucc>()
                val o1isFirst = o[1].isInstanceOfArray<CastClassFirstSucc>()
                r == 1 && o0isSecond && o1isFirst
            },
            { o, r ->
                val o0isSecond = o[0].isInstanceOfArray<CastClassSecondSucc>()
                val o1isFirst = o[1].isInstanceOfArray<CastClassFirstSucc>()
                r == 2 && !o0isSecond && !o1isFirst
            },
            { o, r ->
                val o0isSecond = o[0].isInstanceOfArray<CastClassSecondSucc>()
                val o1isFirst = o[1].isInstanceOfArray<CastClassFirstSucc>()
                r == 3 && o0isSecond && !o1isFirst
            },
            { o, r ->
                val o0isSecond = o[0].isInstanceOfArray<CastClassSecondSucc>()
                val o1isFirst = o[1].isInstanceOfArray<CastClassFirstSucc>()
                r == 4 && !o0isSecond && o1isFirst
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("TODO: Can't deal with complicated expressions")
    fun testInstanceOfAsPartOfInternalExpressionsIntValue() {
        check(
            InstanceOfExample::instanceOfAsPartOfInternalExpressionsIntValue,
            branches = ignoreExecutionsNumber,
            { o, r ->
                val t1 = o.isInstanceOfArray<CastClass>()
                val t2 = !o.isInstanceOfArray<CastClassSecondSucc>()
                val t3 = r == 1
                t1 && t2 && t3
            },
            { o, r -> o.isInstanceOfArray<CastClassSecondSucc>() && r == 2 },
            { o, r -> !o.isInstanceOfArray<CastClass>() && r == 3 },
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("TODO: Zero branches")
    fun testInstanceOfAsInternalExpressionsMap() {
        check(
            InstanceOfExample::instanceOfAsInternalExpressionsMap,
            ge(3),
            coverage = DoNotCalculate
        )
    }


    @Test
    fun testSymbolicInstanceOf() {
        check(
            InstanceOfExample::symbolicInstanceOf,
            eq(6),
            { _, i, r -> i < 1 && r == null },
            { _, i, r -> i > 3 && r == null },
            { o, _, _ -> o == null },
            { o, i, _ -> o != null && i > o.lastIndex },
            { o, i, r -> o != null && o[i] is CastClassFirstSucc && r is CastClassFirstSucc },
            { o, i, r -> o != null && o[i] !is CastClassFirstSucc && r is CastClassSecondSucc },
        )
    }

    @Test
    //TODO: fails without concrete execution
    fun testComplicatedInstanceOf() {
        check(
            InstanceOfExample::complicatedInstanceOf,
            eq(8),
            { _, index, _, result -> index < 0 && result == null },
            { _, index, _, result -> index > 2 && result == null },
            { objects, index, _, result -> index in 0..2 && objects == null && result == null },
            { objects, index, _, result -> index in 0..2 && objects != null && objects.size < index + 2 && result == null },
            { objects, index, objectExample, result ->
                require(objects != null && result != null && objectExample is CastClassFirstSucc)

                val sizeConstraint = index in 0..2 && objects.size >= index + 2
                val resultConstraint = result[index].x == objectExample.z

                sizeConstraint && resultConstraint
            },
            { objects, index, objectExample, _ ->
                index in 0..2 && objects != null && objects.size >= index + 2 && objectExample == null
            },
            { objects, index, objectExample, result ->
                require(objects != null && result != null && result[index] is CastClassSecondSucc)

                val sizeConstraint = index in 0..2 && objects.size >= index + 2
                val typeConstraint = objectExample !is CastClassFirstSucc && result[index] is CastClassSecondSucc
                val resultConstraint = result[index].x == result[index].foo()

                sizeConstraint && typeConstraint && resultConstraint
            },
            { objects, index, objectExample, result ->
                require(objects != null && result != null)

                val sizeConstraint = index in 0..2 && objects.size >= index + 2
                val objectExampleConstraint = objectExample !is CastClassFirstSucc
                val resultTypeConstraint = result[index] !is CastClassFirstSucc && result[index] !is CastClassSecondSucc
                val typeConstraint = objectExampleConstraint && resultTypeConstraint
                val resultConstraint = result[index].x == result[index].foo()

                sizeConstraint && typeConstraint && resultConstraint
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testInstanceOfFromArray() {
        check(
            InstanceOfExample::instanceOfFromArray,
            eq(5),
            { a, _ -> a == null },
            { a, r -> a.size != 3 && r == null },
            { a, r -> a.size == 3 && a[0] is CastClassFirstSucc && r != null && r[0] is CastClassFirstSucc },
            { a, r -> a.size == 3 && a[0] is CastClassSecondSucc && r != null && r[0] == null },
            { a, r -> a.size == 3 && a[0] !is CastClassFirstSucc && a[0] !is CastClassSecondSucc && r != null },
        )
    }

    @Test
    fun testInstanceOfFromArrayWithReadingAnotherElement() {
        check(
            InstanceOfExample::instanceOfFromArrayWithReadingAnotherElement,
            eq(4),
            { a, _ -> a == null },
            { a, r -> a != null && a.size < 2 && r == null },
            { a, r -> a != null && a.size >= 2 && a[0] is CastClassFirstSucc && r is CastClassFirstSucc },
            { a, r -> a != null && a.size >= 2 && a[0] !is CastClassFirstSucc && r == null },
        )
    }

    @Test
    fun testInstanceOfFromArrayWithReadingSameElement() {
        check(
            InstanceOfExample::instanceOfFromArrayWithReadingSameElement,
            eq(4),
            { a, _ -> a == null },
            { a, r -> a != null && a.size < 2 && r == null },
            { a, r -> a != null && a.size >= 2 && a[0] is CastClassFirstSucc && r is CastClassFirstSucc },
            { a, r -> a != null && a.size >= 2 && a[0] !is CastClassFirstSucc && r == null },
        )
    }

    @Test
    fun testIsNull() {
        check(
            InstanceOfExample::isNull,
            eq(2),
            { a, r -> a is Array<*> && a.isArrayOf<Number>() && r == 1 },
            { a, r -> a == null && r == 2 },
        )
    }

    @Test
    fun testArrayInstanceOfArray() {
        check(
            InstanceOfExample::arrayInstanceOfArray,
            eq(4),
            { a, r -> a == null && r == null },
            { a, r -> a is Array<*> && a.isArrayOf<Int>() && r is Array<*> && r.isArrayOf<Int>() },
            { a, r -> a is Array<*> && a.isArrayOf<Double>() && r is Array<*> && r.isArrayOf<Double>() },
            { a, r ->
                a is Array<*> && a.isArrayOf<Number>() && !a.isArrayOf<Int>() &&
                        !a.isArrayOf<Double>() && r is Array<*> && a contentDeepEquals r
            },
        )
    }

    @Test
    fun testObjectInstanceOfArray() {
        check(
            InstanceOfExample::objectInstanceOfArray,
            eq(3),
            { a, r -> a is IntArray && r is IntArray && a contentEquals r },
            { a, r -> a is BooleanArray && r is BooleanArray && a contentEquals r },
            { a, r -> (a == null && r == null) || (!(a is IntArray || a is BooleanArray) && a.equals(r)) },
        )
    }

    @Test
    fun testInstanceOfObjectArray() {
        check(
            InstanceOfExample::instanceOfObjectArray,
            eq(3),
            { a, r -> a == null && r == null },
            { a, r -> a is Array<*> && a.isArrayOf<Array<IntArray>>() && r is Array<*> && r contentDeepEquals a },
            { a, r -> a is Array<*> && !a.isArrayOf<Array<IntArray>>() && r!!::class == a::class },
        )
    }


    private inline fun <reified T : Any> Any?.isInstanceOfArray() =
        (this as? Array<*>)?.run { T::class.java.isAssignableFrom(this::class.java.componentType) } == true
}