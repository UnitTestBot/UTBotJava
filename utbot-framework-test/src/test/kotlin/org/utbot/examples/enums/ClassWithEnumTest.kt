package org.utbot.examples.enums

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.examples.enums.ClassWithEnum.StatusEnum.ERROR
import org.utbot.examples.enums.ClassWithEnum.StatusEnum.READY
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.id
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.util.jField
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withPushingStateFromPathSelectorForConcrete
import org.utbot.testcheckers.withoutConcrete

class ClassWithEnumTest : UtValueTestCaseChecker(testClass = ClassWithEnum::class) {
    @Test
    fun testOrdinal() {
        withoutConcrete {
            checkAllCombinations(ClassWithEnum::useOrdinal)
        }
    }

    @Test
    fun testGetter() {
        check(
            ClassWithEnum::useGetter,
            eq(2),
            { s, r -> s == null && r == -1 },
            { s, r -> s != null && r == 0 },
        )
    }

    @Test
    fun testDifficultIfBranch() {
        check(
            ClassWithEnum::useEnumInDifficultIf,
            eq(2),
            { s, r -> s.equals("TRYIF", ignoreCase = true) && r == 1 },
            { s, r -> !s.equals("TRYIF", ignoreCase = true) && r == 2 },
        )
    }

    @Test
    @Disabled("TODO JIRA:1686")
    fun testNullParameter() {
        check(
            ClassWithEnum::nullEnumAsParameter,
            eq(3),
            { e, _ -> e == null },
            { e, r -> e == READY && r == 0 },
            { e, r -> e == ERROR && r == -1 },
        )
    }

    @Test
    @Disabled("TODO JIRA:1686")
    fun testNullField() {
        checkWithException(
            ClassWithEnum::nullField,
            eq(3),
            { e, r -> e == null && r.isException<NullPointerException>() },
            { e, r -> e == ERROR && r.isException<NullPointerException>() },
            { e, r -> e == READY && r.getOrNull()!! == 3 && READY.s.length == 3 },
        )
    }

    @Test
    @Disabled("TODO JIRA:1686")
    fun testChangeEnum() {
        checkWithException(
            ClassWithEnum::changeEnum,
            eq(3),
            { e, r -> e == null && r.isException<NullPointerException>() },
            { e, r -> e == READY && r.getOrNull()!! == ERROR.ordinal },
            { e, r -> e == ERROR && r.getOrNull()!! == READY.ordinal },
        )
    }

    @Test
    fun testChangeMutableField() {
        // TODO testing code generation for this method is disabled because we need to restore original field state
        //  should be enabled after solving JIRA:1648
        withEnabledTestingCodeGeneration(testCodeGeneration = false) {
            checkWithException(
                ClassWithEnum::changeMutableField,
                eq(2),
                { e, r -> e == READY && r.getOrNull()!! == 2 },
                { e, r -> (e == null || e == ERROR) && r.getOrNull()!! == -2 },
            )
        }
    }

    @Test
    @Disabled("TODO JIRA:1686")
    fun testCheckName() {
        check(
            ClassWithEnum::checkName,
            eq(3),
            { s, _ -> s == null },
            { s, r -> s == READY.name && r == ERROR.name },
            { s, r -> s != READY.name && r == READY.name },
        )
    }

    @Test
    fun testChangingStaticWithEnumInit() {
        checkThisAndStaticsAfter(
            ClassWithEnum::changingStaticWithEnumInit,
            eq(1),
            { t, staticsAfter, r ->
                // for some reasons x is inaccessible
                val x = FieldId(t.javaClass.id, "x").jField.get(t) as Int

                val y = staticsAfter[FieldId(ClassWithEnum.ClassWithStaticField::class.id, "y")]!!.value as Int

                val areStaticsCorrect = x == 1 && y == 11
                areStaticsCorrect && r == true
            }
        )
    }

    @Test
    fun testEnumValues() {
        checkStaticMethod(
            ClassWithEnum.StatusEnum::values,
            eq(1),
            { r -> r.contentEquals(arrayOf(READY, ERROR)) },
        )
    }

    @Test
    fun testFromCode() {
        checkStaticMethod(
            ClassWithEnum.StatusEnum::fromCode,
            eq(3),
            { code, r -> code == 10 && r == READY },
            { code, r -> code == -10 && r == ERROR },
            { code, r -> code !in setOf(10, -10) && r == null }, // IllegalArgumentException
        )
    }

    @Test
    fun testFromIsReady() {
        checkStaticMethod(
            ClassWithEnum.StatusEnum::fromIsReady,
            eq(2),
            { isFirst, r -> isFirst && r == READY },
            { isFirst, r -> !isFirst && r == ERROR },
        )
    }

    @Test
    @Disabled("TODO JIRA:1450")
    fun testPublicGetCodeMethod() {
        checkWithThis(
            ClassWithEnum.StatusEnum::publicGetCode,
            eq(2),
            { enumInstance, r -> enumInstance == READY && r == 10 },
            { enumInstance, r -> enumInstance == ERROR && r == -10 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testImplementingInterfaceEnumInDifficultBranch() {
        withPushingStateFromPathSelectorForConcrete {
            check(
                ClassWithEnum::implementingInterfaceEnumInDifficultBranch,
                eq(2),
                { s, r -> s.equals("SUCCESS", ignoreCase = true) && r == 0 },
                { s, r -> !s.equals("SUCCESS", ignoreCase = true) && r == 2 },
            )
        }
    }

    @Test
    fun testAffectSystemStaticAndUseInitEnumFromIt() {
        check(
            ClassWithEnum::affectSystemStaticAndInitEnumFromItAndReturnField,
            eq(1),
            { r -> r == true },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testAffectSystemStaticAndInitEnumFromItAndGetItFromEnumFun() {
        check(
            ClassWithEnum::affectSystemStaticAndInitEnumFromItAndGetItFromEnumFun,
            eq(1),
            { r -> r == true },
            coverage = DoNotCalculate
        )
    }
}