package org.utbot.examples.collections

import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.between
import org.utbot.tests.infrastructure.isException
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation SAT-1332
internal class ListsPart3Test : UtValueTestCaseChecker(
    testClass = Lists::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun createTest() {
        check(
            Lists::create,
            eq(3),
            { a, _ -> a == null },
            { a, r -> a != null && a.isEmpty() && r!!.isEmpty() },
            { a, r -> a != null && a.isNotEmpty() && r != null && r.isNotEmpty() && a.toList() == r.also { println(r) } },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testBigListFromParameters() {
        check(
            Lists::bigListFromParameters,
            eq(1),
            { list, r -> list.size == r && list.size == 11 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetNonEmptyCollection() {
        check(
            Lists::getNonEmptyCollection,
            eq(3),
            { collection, _ -> collection == null },
            { collection, r -> collection.isEmpty() && r == null },
            { collection, r -> collection.isNotEmpty() && collection == r },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetFromAnotherListToArray() {
        check(
            Lists::getFromAnotherListToArray,
            eq(4),
            { l, _ -> l == null },
            { l, _ -> l.isEmpty() },
            { l, r -> l[0] == null && r == null },
            { l, r -> l[0] != null && r is Array<*> && r.isArrayOf<Int>() && r.size == 1 && r[0] == l[0] },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun addElementsTest() {
        check(
            Lists::addElements,
            eq(5),
            { list, _, _ -> list == null },
            { list, a, _ -> list != null && list.size >= 2 && a == null },
            { list, _, r -> list.size < 2 && r == list },
            { list, a, r -> list.size >= 2 && a.size < 2 && r == list },
            { list, a, r ->
                require(r != null)

                val sizeConstraint = list.size >= 2 && a.size >= 2 && r.size == list.size + a.size
                val content = r.mapIndexed { i, it -> if (i < r.size) it == r[i] else it == a[i - r.size] }.all { it }

                sizeConstraint && content
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun removeElementsTest() {
        checkWithException(
            Lists::removeElements,
            between(7..8),
            { list, _, _, r -> list == null && r.isException<NullPointerException>() },
            { list, i, _, r -> list != null && i < 0 && r.isException<IndexOutOfBoundsException>() },
            { list, i, _, r -> list != null && i >= 0 && list.size > i && list[i] == null && r.isException<NullPointerException>() },
            { list, i, j, r ->
                require(list != null && list[i] != null)

                val listConstraints = i >= 0 && list.size > i && (list.size <= j + 1 || j < 0)
                val resultConstraint = r.isException<IndexOutOfBoundsException>()

                listConstraints && resultConstraint
            },
            { list, i, j, r ->
                require(list != null && list[i] != null)

                val k = j + if (i <= j) 1 else 0
                val indicesConstraint = i >= 0 && list.size > i && j >= 0 && list.size > j + 1
                val contentConstraint = list[i] != null && list[k] == null
                val resultConstraint = r.isException<NullPointerException>()

                indicesConstraint && contentConstraint && resultConstraint
            },
            { list, i, j, r ->
                require(list != null)

                val k = j + if (i <= j) 1 else 0

                val precondition = i >= 0 && list.size > i && j >= 0 && list.size > j + 1 && list[i] < list[k]
                val postcondition = r.getOrNull() == list[i]

                precondition && postcondition
            },
            { list, i, j, r ->
                require(list != null)

                val k = j + if (i <= j) 1 else 0

                val precondition = i >= 0 && list.size > i && j >= 0 && list.size > j + 1 && list[i] >= list[k]
                val postcondition = r.getOrNull() == list[k]

                precondition && postcondition
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun createArrayWithDifferentTypeTest() {
        check(
            Lists::createWithDifferentType,
            eq(2),
            { x, r -> x % 2 != 0 && r is java.util.LinkedList && r == List(4) { it } },
            { x, r -> x % 2 == 0 && r is java.util.ArrayList && r == List(4) { it } },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun getElementsTest() {
        check(
            Lists::getElements,
            eq(4),
            { x, _ -> x == null },
            { x, r -> x != null && x.isEmpty() && r!!.isEmpty() },
            { x, _ -> x != null && x.isNotEmpty() && x.any { it == null } },
            { x, r -> x != null && x.isNotEmpty() && x.all { it is Int } && r!!.toList() == x },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun setElementsTest() {
        check(
            Lists::setElements,
            eq(3),
            { x, _ -> x == null },
            { x, r -> x != null && x.isEmpty() && r!!.isEmpty() },
            { x, r -> x != null && x.isNotEmpty() && r!!.containsAll(x.toList()) && r.size == x.size },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testClear() {
        check(
            Lists::clear,
            eq(3),
            { list, _ -> list == null },
            { list, r -> list.size >= 2 && r == emptyList<Int>() },
            { list, r -> list.size < 2 && r == emptyList<Int>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testAddAll() {
        check(
            Lists::addAll,
            eq(3),
            { list, _, _ -> list == null },
            { list, i, r ->
                list != null && list.isEmpty() && r != null && r.size == 1 && r[0] == i
            },
            { list, i, r ->
                list != null && list.isNotEmpty() && r != null && r.size == 1 + list.size && r == listOf(i) + list
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testAddAllInIndex() {
        check(
            Lists::addAllByIndex,
            eq(4),
            { list, i, _ -> list == null && i >= 0 },
            { list, i, _ -> list == null && i < 0 },
            { list, i, r -> list != null && i >= list.size && r == list },
            { list, i, r ->
                list != null && i in 0..list.lastIndex && r == list.toMutableList().apply { addAll(i, listOf(0, 1)) }
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("TODO: add choosing proper type in list wrapper")
    fun testRemoveFromList() {
        checkWithException(
            Lists::removeFromList,
            ge(4),
            { list, _, r -> list == null && r.isException<NullPointerException>() },
            { list, _, r -> list != null && list.isEmpty() && r.isException<IndexOutOfBoundsException>() },
            { list, i, r ->
                require(list != null && list.lastOrNull() != null)

                list.isNotEmpty() && (i < 0 || i >= list.size) && r.isException<IndexOutOfBoundsException>()
            },
            { list, i, r ->
                require(list != null && list.lastOrNull() != null)

                val changedList = list.toMutableList().apply {
                    set(i, last())
                    removeLast()
                }

                val precondition = list.isNotEmpty() && i >= 0 && i < list.size
                val postcondition = changedList == r.getOrNull()

                precondition && postcondition
            },
            // TODO: add branches with conditions (list is LinkedList) and (list !is ArrayList && list !is LinkedList)
            coverage = DoNotCalculate
        )
    }

}