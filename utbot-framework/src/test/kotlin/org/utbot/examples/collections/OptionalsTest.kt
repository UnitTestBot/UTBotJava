package org.utbot.examples.collections

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.between
import org.utbot.examples.eq
import org.utbot.examples.ignoreExecutionsNumber
import org.utbot.examples.isException
import org.utbot.examples.singleValue
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test

class OptionalsTest : UtValueTestCaseChecker(
    Optionals::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {


    @Test
    fun testCreate() {
        checkWithException(
            Optionals::create,
            eq(2),
            { value, result -> value == null && result.isException<NullPointerException>() },
            { value, result -> value != null && result.getOrNull()!!.get() == value },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCreateInt() {
        check(
            Optionals::createInt,
            eq(1),
            { value, result -> result!!.asInt == value },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCreateLong() {
        check(
            Optionals::createLong,
            eq(1),
            { value, result -> result!!.asLong == value },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCreateDouble() {
        check(
            Optionals::createDouble,
            eq(1),
            { value, result -> result!!.asDouble == value || result.asDouble.isNaN() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCreateNullable() {
        checkStatics(
            Optionals::createNullable,
            eq(2),
            { value, statics, result -> value == null && result === statics.singleValue() },
            { value, _, result -> value != null && result!!.get() == value },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCreateEmpty() {
        checkStatics(
            Optionals::createEmpty,
            eq(1),
            { statics, result -> result === statics.singleValue() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCreateIntEmpty() {
        checkStatics(
            Optionals::createIntEmpty,
            eq(1),
            { statics, result -> result === statics.singleValue() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCreateLongEmpty() {
        checkStatics(
            Optionals::createLongEmpty,
            eq(1),
            { statics, result -> result === statics.singleValue() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCreateDoubleEmpty() {
        checkStatics(
            Optionals::createDoubleEmpty,
            eq(1),
            { statics, result -> result === statics.singleValue() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetValue() {
        checkStatics(
            Optionals::getValue,
            eq(3),
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional != null && optional === statics.singleValue() && result == null },
            { optional, _, result -> optional != null && result == optional.get() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetIntValue() {
        checkStatics(
            Optionals::getIntValue,
            eq(3),
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional != null && optional === statics.singleValue() && result == null },
            { optional, _, result -> optional != null && result == optional.asInt },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetLongValue() {
        checkStatics(
            Optionals::getLongValue,
            eq(3),
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional != null && optional === statics.singleValue() && result == null },
            { optional, _, result -> optional != null && result == optional.asLong },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetDoubleValue() {
        checkStatics(
            Optionals::getDoubleValue,
            eq(3),
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional != null && optional === statics.singleValue() && result == null },
            { optional, _, result -> optional != null && result == optional.asDouble },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetWithIsPresent() {
        checkStatics(
            Optionals::getWithIsPresent,
            eq(3),
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional === statics.singleValue() && result == null },
            { optional, _, result -> optional.get() == result },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCountIfPresent() {
        checkStatics(
            Optionals::countIfPresent,
            eq(3),
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional === statics.singleValue() && result == 0 },
            { optional, _, result -> optional.get() == result },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCountIntIfPresent() {
        checkStatics(
            Optionals::countIntIfPresent,
            ignoreExecutionsNumber,
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional === statics.singleValue() && result == 0 },
            { optional, _, result -> optional.asInt == result },
        )
    }

    @Test
    fun testCountLongIfPresent() {
        checkStatics(
            Optionals::countLongIfPresent,
            ignoreExecutionsNumber,
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional === statics.singleValue() && result == 0L },
            { optional, _, result -> optional.asLong == result },
        )
    }

    @Test
    fun testCountDoubleIfPresent() {
        checkStatics(
            Optionals::countDoubleIfPresent,
            ignoreExecutionsNumber,
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional === statics.singleValue() && result == 0.0 },
            { optional, _, result -> optional.asDouble == result },
        )
    }

    @Test
    fun testFilterLessThanZero() {
        checkStatics(
            Optionals::filterLessThanZero,
            eq(4),
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional === statics.singleValue() && result === optional },
            { optional, _, result -> optional.get() >= 0 && result == optional },
            { optional, statics, result -> optional.get() < 0 && result === statics.singleValue() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testAbsNotNull() {
        checkStatics(
            Optionals::absNotNull,
            eq(4),
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional === statics.singleValue() && result === optional },
            { optional, _, result -> optional.get() < 0 && result!!.get() == -optional.get() },
            { optional, _, result -> optional.get() >= 0 && result == optional },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testMapLessThanZeroToNull() {
        checkStatics(
            Optionals::mapLessThanZeroToNull,
            eq(4),
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional === statics.singleValue() && result === optional },
            { optional, statics, result -> optional.get() < 0 && result === statics.singleValue() },
            { optional, _, result -> optional.get() >= 0 && result == optional },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testFlatAbsNotNull() {
        checkStatics(
            Optionals::flatAbsNotNull,
            eq(4),
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional === statics.singleValue() && result === optional },
            { optional, _, result -> optional.get() < 0 && result!!.get() == -optional.get() },
            { optional, _, result -> optional.get() >= 0 && result == optional },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testFlatMapWithNull() {
        checkStatics(
            Optionals::flatMapWithNull,
            eq(5),
            { optional, _, _ -> optional == null },
            { optional, statics, result -> optional === statics.singleValue() && result === optional },
            { optional, statics, result -> optional.get() < 0 && result === statics.singleValue() },
            { optional, _, result -> optional.get() > 0 && result == optional },
            { optional, _, result -> optional.get() == 0 && result == null },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testLeftOrElseRight() {
        checkStatics(
            Optionals::leftOrElseRight,
            eq(3),
            { left, _, _, _ -> left == null },
            { left, right, statics, result -> left === statics.singleValue() && result == right },
            { left, _, _, result -> left.isPresent && result == left.get() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testLeftIntOrElseRight() {
        checkStatics(
            Optionals::leftIntOrElseRight,
            eq(3),
            { left, _, _, _ -> left == null },
            { left, right, statics, result -> left === statics.singleValue() && result == right },
            { left, _, _, result -> left.isPresent && result == left.asInt },
            coverage = DoNotCalculate
        )
    }


    @Test
    fun testLeftLongOrElseRight() {
        checkStatics(
            Optionals::leftLongOrElseRight,
            eq(3),
            { left, _, _, _ -> left == null },
            { left, right, statics, result -> left === statics.singleValue() && result == right },
            { left, _, _, result -> left.isPresent && result == left.asLong },
            coverage = DoNotCalculate
        )
    }


    @Test
    fun testLeftDoubleOrElseRight() {
        checkStatics(
            Optionals::leftDoubleOrElseRight,
            eq(3),
            { left, _, _, _ -> left == null },
            { left, right, statics, result -> left === statics.singleValue() && (result == right || result!!.isNaN() && right.isNaN()) },
            { left, _, _, result -> left.isPresent && (result == left.asDouble || result!!.isNaN() && left.asDouble.isNaN()) },
            coverage = DoNotCalculate
        )
    }


    @Test
    fun testLeftOrElseGetOne() {
        checkStatics(
            Optionals::leftOrElseGetOne,
            eq(3),
            { left, _, _ -> left == null },
            { left, statics, result -> left === statics.singleValue() && result == 1 },
            { left, _, result -> left.isPresent && result == left.get() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testLeftIntOrElseGetOne() {
        checkStatics(
            Optionals::leftIntOrElseGetOne,
            eq(3),
            { left, _, _ -> left == null },
            { left, statics, result -> left === statics.singleValue() && result == 1 },
            { left, _, result -> left.isPresent && result == left.asInt },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testLeftLongOrElseGetOne() {
        checkStatics(
            Optionals::leftLongOrElseGetOne,
            eq(3),
            { left, _, _ -> left == null },
            { left, statics, result -> left === statics.singleValue() && result == 1L },
            { left, _, result -> left.isPresent && result == left.asLong },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testLeftDoubleOrElseGetOne() {
        checkStatics(
            Optionals::leftDoubleOrElseGetOne,
            eq(3),
            { left, _, _ -> left == null },
            { left, statics, result -> left === statics.singleValue() && result == 1.0 },
            { left, _, result -> left.isPresent && result == left.asDouble },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testLeftOrElseThrow() {
        checkStatics(
            Optionals::leftOrElseThrow,
            eq(3),
            { left, _, _ -> left == null },
            { left, statics, result -> left === statics.singleValue() && result == null },
            { left, _, result -> left.isPresent && result == left.get() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testLeftIntOrElseThrow() {
        checkStatics(
            Optionals::leftIntOrElseThrow,
            eq(3),
            { left, _, _ -> left == null },
            { left, statics, result -> left === statics.singleValue() && result == null },
            { left, _, result -> left.isPresent && result == left.asInt },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testLeftLongOrElseThrow() {
        checkStatics(
            Optionals::leftLongOrElseThrow,
            eq(3),
            { left, _, _ -> left == null },
            { left, statics, result -> left === statics.singleValue() && result == null },
            { left, _, result -> left.isPresent && result == left.asLong },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testLeftDoubleOrElseThrow() {
        checkStatics(
            Optionals::leftDoubleOrElseThrow,
            eq(3),
            { left, _, _ -> left == null },
            { left, statics, result -> left === statics.singleValue() && result == null },
            { left, _, result -> left.isPresent && result == left.asDouble },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testEqualOptionals() {
        check(
            Optionals::equalOptionals,
            between(4..7),
            { left, _, result -> left == null && result == null },
            { left, right, result -> left != null && left != right && !result!! },
            { left, right, result -> left != null && left === right && !left.isPresent && !right.isPresent && result!! },
            { left, right, result -> left != null && left == right && left.isPresent && right.isPresent && result!! },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testEqualOptionalsInt() {
        check(
            Optionals::equalOptionalsInt,
            between(4..8),
            { left, _, result -> left == null && result == null },
            { left, right, result -> left != null && left != right && !result!! },
            { left, right, result -> left != null && left === right && !left.isPresent && !right.isPresent && result!! },
            { left, right, result -> left != null && left == right && left.isPresent && right.isPresent && result!! },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testEqualOptionalsLong() {
        check(
            Optionals::equalOptionalsLong,
            between(4..8),
            { left, _, result -> left == null && result == null },
            { left, right, result -> left != null && left != right && !result!! },
            { left, right, result -> left != null && left === right && !left.isPresent && !right.isPresent && result!! },
            { left, right, result -> left != null && left == right && left.isPresent && right.isPresent && result!! },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testEqualOptionalsDouble() {
        check(
            Optionals::equalOptionalsDouble,
            between(4..8),
            { left, _, result -> left == null && result == null },
            { left, right, result -> left != null && left != right && !result!! },
            { left, right, result -> left != null && left === right && !left.isPresent && !right.isPresent && result!! },
            { left, right, result -> left != null && left == right && left.isPresent && right.isPresent && result!! },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testOptionalOfPositive() {
        check(
            Optionals::optionalOfPositive,
            eq(2),
            { value, result -> value > 0 && result != null && result.isPresent && result.get() == value },
            { value, result -> value <= 0 && result != null && !result.isPresent }
        )
    }
}