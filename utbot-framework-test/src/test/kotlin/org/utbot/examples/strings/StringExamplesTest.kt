package org.utbot.examples.strings

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.atLeast
import org.utbot.tests.infrastructure.between
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.utbot.tests.infrastructure.keyMatch
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testcheckers.withPushingStateFromPathSelectorForConcrete
import org.utbot.testcheckers.withSolverTimeoutInMillis
import org.utbot.testcheckers.withoutMinimization
import org.utbot.tests.infrastructure.CodeGeneration

internal class StringExamplesTest : UtValueTestCaseChecker(
    testClass = StringExamples::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    @Disabled("Flaky test: https://github.com/UnitTestBot/UTBotJava/issues/131 (will be enabled in new strings PR)")
    fun testByteToString() {
        // TODO related to the https://github.com/UnitTestBot/UTBotJava/issues/131
        withSolverTimeoutInMillis(5000) {
            check(
                StringExamples::byteToString,
                eq(2),
                { a, b, r -> a > b && r == a.toString() },
                { a, b, r -> a <= b && r == b.toString() },
            )
        }
    }

    @Test
    fun testReplace() {
        check(
            StringExamples::replace,
            between(3..4),  // replace with eq when JIRA:1475 fixed
            { fst, _, _ -> fst == null },
            { fst, snd, _ -> fst != null && snd == null },
            { fst, snd, r -> fst != null && snd != null && r != null && (!r.contains("abc") || snd == "abc") },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testShortToString() {
        // TODO related to the https://github.com/UnitTestBot/UTBotJava/issues/131
        withSolverTimeoutInMillis(5000) {
            check(
                StringExamples::shortToString,
                eq(2),
                { a, b, r -> a > b && r == a.toString() },
                { a, b, r -> a <= b && r == b.toString() },
            )
        }
    }


    @Test
    fun testIntToString() {
        // TODO related to the https://github.com/UnitTestBot/UTBotJava/issues/131
        withSolverTimeoutInMillis(5000) {
            check(
                StringExamples::intToString,
                ignoreExecutionsNumber,
                { a, b, r -> a > b && r == a.toString() },
                { a, b, r -> a <= b && r == b.toString() },
            )
        }
    }


    @Test
    fun testLongToString() {
        // TODO related to the https://github.com/UnitTestBot/UTBotJava/issues/131
        withSolverTimeoutInMillis(5000) {
            check(
                StringExamples::longToString,
                ignoreExecutionsNumber,
                { a, b, r -> a > b && r == a.toString() },
                { a, b, r -> a <= b && r == b.toString() },
            )
        }
    }

    @Test
    fun testStartsWithLiteral() {
        check(
            StringExamples::startsWithLiteral,
            ge(4), // replace with eq when JIRA:1475 fixed
            { v, _ -> v == null },
            { v, r -> v != null && v.startsWith("1234567890") && r!!.startsWith("12a4567890") },
            { v, r -> v != null && v[0] == 'x' && r!![0] == 'x' },
            { v, r -> v != null && v.toLowerCase() == r }
        )
    }

    @Test
    fun testBooleanToString() {
        check(
            StringExamples::booleanToString,
            eq(2),
            { a, b, r -> a == b && r == "false" },
            { a, b, r -> a != b && r == "true" },
        )
    }


    @Test
    fun testCharToString() {
        // TODO related to the https://github.com/UnitTestBot/UTBotJava/issues/131
        withSolverTimeoutInMillis(5000) {
            check(
                StringExamples::charToString,
                eq(2),
                { a, b, r -> a > b && r == a.toString() },
                { a, b, r -> a <= b && r == b.toString() },
            )
        }
    }


    @Test
    @Disabled("TODO:add Byte.valueOf(String) support")
    fun testStringToByte() {
        check(
            StringExamples::stringToByte,
            eq(-1),
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("TODO:add Short.valueOf(String) support")
    fun testStringToShort() {
        check(
            StringExamples::stringToShort,
            eq(-1),
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("TODO:add Integer.valueOf(String) support")
    fun testStringToInt() {
        check(
            StringExamples::stringToInt,
            eq(-1),
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("TODO:add Long.valueOf support")
    fun testStringToLong() {
        check(
            StringExamples::stringToLong,
            eq(-1),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testStringToBoolean() {
        check(
            StringExamples::stringToBoolean,
            ge(2),
            { s, r -> (s == null || r == java.lang.Boolean.valueOf(s)) && r == false}, // minimization
            { s, r -> s != null && r == true && r == java.lang.Boolean.valueOf(s) },
        )
    }

    @Test
    fun testConcat() {
        check(
            StringExamples::concat,
            between(1..2),
            { fst, snd, r -> fst == null || snd == null && r == fst + snd },
            { fst, snd, r -> r == fst + snd },
        )
    }

    @Test
    @Disabled("Sometimes it freezes the execution for several hours JIRA:1453")
    fun testConcatWithObject() {
        check(
            StringExamples::concatWithObject,
            between(2..3),
            { pair, r -> pair == null && r == "fst.toString() = $pair" },
            { pair, r -> pair != null && r == "fst.toString() = $pair" }
        )
    }

    @Test
    fun testStringConstants() {
        check(
            StringExamples::stringConstants,
            between(1..2),
            { s, r -> r == "String('$s')" },
        )
    }

    @Test
    fun testContainsOnLiterals() {
        check(
            StringExamples::containsOnLiterals,
            eq(1),
        )
    }

    @Test
    @Disabled("This is a flaky test")
    fun testConcatWithInt() {
        check(
            StringExamples::concatWithInts,
            eq(3),
            { a, b, r -> a == b && r == null }, // IllegalArgumentException
            { a, b, r -> a > b && r == "a > b, a:$a, b:$b" },
            { a, b, r -> a < b && r == "a < b, a:$a, b:$b" },
        )
    }

    @Test
    fun testUseStringBuffer() {
        check(
            StringExamples::useStringBuffer,
            between(1..2),
            { fst, snd, r -> r == "$fst, $snd" },
        )
    }

    @Test
    fun testNullableStringBuffer() {
        checkWithException(
            StringExamples::nullableStringBuffer,
            eq(4),
            { _, i, r -> i >= 0 && r.isException<NullPointerException>() },
            { _, i, r -> i < 0 && r.isException<NullPointerException>() },
            { buffer, i, r -> i >= 0 && r.getOrNull() == "${buffer}Positive" },
            { buffer, i, r -> i < 0 && r.getOrNull() == "${buffer}Negative" },
        )
    }

    @Test
    @Disabled("Flaky on GitHub: https://github.com/UnitTestBot/UTBotJava/issues/1004")
    fun testIsValidUuid() {
        val pattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        check(
            StringExamples::isValidUuid,
            ignoreExecutionsNumber,
            { uuid, r -> uuid == null || uuid.length == 0 && r == false },
            { uuid, r -> uuid.length > 0 && uuid.isBlank() && r == false },
            { uuid, r -> uuid.length > 0 && uuid.isNotBlank() && r == false },
            { uuid, r -> uuid.length > 1 && uuid.isNotBlank() && !uuid.matches(pattern) && r == false },
            { uuid, r -> uuid.length > 1 && uuid.isNotBlank() && uuid.matches(pattern) && r == true },
        )
    }

    @Test
    fun testIsValidUuidShortVersion() {
        val pattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        check(
            StringExamples::isValidUuidShortVersion,
            eq(3),
            { uuid, r -> uuid == null && r == false },
            { uuid, r -> uuid.matches(pattern) && r == true },
            { uuid, r -> !uuid.matches(pattern) && r == false },
        )
    }

    @Test
    fun testIsBlank() {
        check(
            StringExamples::isBlank,
            ge(4),
            { cs, r -> cs == null && r == true },
            { cs, r -> cs.length == 0 && r == true },
            { cs, r -> cs.length > 0 && cs.isBlank() && r == true },
            { cs, r -> cs.length > 0 && cs.isNotBlank() && r == false },
            summaryNameChecks = listOf(
                keyMatch("testIsBlank_StrLenEqualsZero"),
                keyMatch("testIsBlank_NotCharacterIsWhitespace"),
                keyMatch("testIsBlank_CharacterIsWhitespace")
            )
        )
    }

    @Test
    fun testLength() {
        check(
            StringExamples::length,
            eq(2), // TODO: that strange, why we haven't 3rd option?
            { cs, r -> cs == null && r == 0 },
            { cs, r -> cs != null && r == cs.length },
        )
    }

    @Test
    fun testLonger() {
        checkWithException(
            StringExamples::longer,
            ignoreExecutionsNumber,
            { _, i, r -> i <= 0 && r.isException<IllegalArgumentException>() },
            { cs, i, r -> i > 0 && cs == null && !r.getOrThrow() },
            { cs, i, r -> i > 0 && cs != null && cs.length > i && r.getOrThrow() },
            coverage = DoNotCalculate // TODO: Coverage calculation fails in the child process with Illegal Argument Exception
        )
    }

    @Test
    fun testEqualChar() {
        checkWithException(
            StringExamples::equalChar,
            eq(4),
            { cs, r -> cs == null && r.isException<NullPointerException>() },
            { cs, r -> cs.isEmpty() && r.isException<StringIndexOutOfBoundsException>() },
            { cs, r -> cs.isNotEmpty() && cs[0] == 'a' && r.getOrThrow() },
            { cs, r -> cs.isNotEmpty() && cs[0] != 'a' && !r.getOrThrow() },
        )
    }

    @Test
    fun testSubstring() {
        checkWithException(
            StringExamples::substring,
            between(5..7),
            { s, _, r -> s == null && r.isException<NullPointerException>() },
            { s, i, r -> s != null && i < 0 || i > s.length && r.isException<StringIndexOutOfBoundsException>() },
            { s, i, r -> s != null && i in 0..s.length && r.getOrThrow() == s.substring(i) && s.substring(i) != "password" },
            { s, i, r -> s != null && i == 0 && r.getOrThrow() == s.substring(i) && s.substring(i) == "password" },
            { s, i, r -> s != null && i != 0 && r.getOrThrow() == s.substring(i) && s.substring(i) == "password" },
        )
    }

    @Test
    fun testSubstringWithEndIndex() {
        checkWithException(
            StringExamples::substringWithEndIndex,
            ignoreExecutionsNumber,
            { s, _, _, r -> s == null && r.isException<NullPointerException>() },
            { s, b, e, r -> s != null && b < 0 || e > s.length || b > e && r.isException<StringIndexOutOfBoundsException>() },
            { s, b, e, r -> r.getOrThrow() == s.substring(b, e) && s.substring(b, e) != "password" },
            { s, b, e, r -> b == 0 && r.getOrThrow() == s.substring(b, e) && s.substring(b, e) == "password" },
            { s, b, e, r ->
                b != 0 && e == s.length && r.getOrThrow() == s.substring(b, e) && s.substring(b, e) == "password"
            },
            { s, b, e, r ->
                b != 0 && e != s.length && r.getOrThrow() == s.substring(b, e) && s.substring(b, e) == "password"
            },
        )
    }

    @Test
    fun testSubstringWithEndIndexNotEqual() {
        checkWithException(
            StringExamples::substringWithEndIndexNotEqual,
            ignoreExecutionsNumber,
            { s, _, r -> s == null && r.isException<NullPointerException>() },
            { s, e, r -> s != null && e < 1 || e > s.length && r.isException<StringIndexOutOfBoundsException>() },
            { s, e, r -> s != null && r.getOrThrow() == s.substring(1, e) },
        )
    }

    @Test
    fun testFullSubstringEquality() {
        checkWithException(
            StringExamples::fullSubstringEquality,
            eq(2),
            { s, r -> s == null && r.isException<NullPointerException>() },
            { s, r -> s != null && r.getOrThrow() },
        )
    }

    @Test
    @Disabled("TODO: add intern support")
    fun testUseIntern() {
        checkWithException(
            StringExamples::useIntern,
            eq(3),
            { s, r -> s == null && r.isException<NullPointerException>() },
            { s, r -> s != null && s != "abc" && r.getOrThrow() == 1 },
            { s, r -> s != null && s == "abc" && r.getOrThrow() == 3 },
            coverage = atLeast(66)
        )
    }

    @Test
    fun testPrefixAndSuffix() {
        check(
            StringExamples::prefixAndSuffix,
            eq(6),
            { s, r -> s == null && r == null }, // NullPointerException
            { s, r -> s.length != 5 && r == 0 },
            { s, r -> s.length == 5 && !s.startsWith("ab") && r == 1 },
            { s, r -> s.length == 5 && s.startsWith("ab") && !s.endsWith("de") && r == 2 },
            { s, r -> s.length == 5 && s.startsWith("ab") && s.endsWith("de") && !s.contains("+") && r == 4 },
            { s, r -> s.length == 5 && s == "ab+de" && r == 3 },
        )
    }

    @Test
    fun testPrefixWithTwoArgs() {
        checkWithException(
            StringExamples::prefixWithTwoArgs,
            between(3..4),
            { s, r -> s == null && r.isException<NullPointerException>() },
            { s, r -> s != null && s.startsWith("abc", 1) && r.getOrThrow() == 1 },
            { s, r -> s != null && !s.startsWith("abc", 1) && r.getOrThrow() == 2 },
        )
    }

    @Test
    fun testPrefixWithOffset() {
        withoutMinimization {
            check(
                StringExamples::prefixWithOffset,
                eq(4), // should be 4, but path selector eliminates several results with false
                { o, r -> o < 0 && r == 2 },
                { o, r -> o > "babc".length - "abc".length && r == 2 },
                { o, r -> o in 0..1 && !"babc".startsWith("abc", o) && r == 2 },
                { o, r -> "babc".startsWith("abc", o) && r == 1 },
            )
        }
    }

    @Test
    fun testStartsWith() {
        check(
            StringExamples::startsWith,
            between(5..6),
            { _, prefix, _ -> prefix == null },
            { _, prefix, _ -> prefix != null && prefix.length < 2 },
            { s, prefix, _ -> prefix != null && prefix.length >= 2 && s == null },
            { s, prefix, r -> prefix != null && prefix.length >= 2 && s != null && s.startsWith(prefix) && r == true },
            { s, prefix, r -> prefix != null && prefix.length >= 2 && s != null && !s.startsWith(prefix) && r == false }

        )
    }

    @Test
    fun testStartsWithOffset() {
        check(
            StringExamples::startsWithOffset,
            between(6..10),
            { _, prefix, _, _ -> prefix == null },
            { _, prefix, _, _ -> prefix != null && prefix.length < 2 },
            { s, prefix, _, _ -> prefix != null && prefix.length >= 2 && s == null },
            { s, prefix, o, r ->
                prefix != null && prefix.length >= 2 && s != null && s.startsWith(prefix, o) && o > 0 && r == 0
            },
            { s, prefix, o, r ->
                prefix != null && prefix.length >= 2 && s != null && s.startsWith(prefix, o) && o == 0 && r == 1
            },
            { s, prefix, o, r ->
                prefix != null && prefix.length >= 2 && s != null && !s.startsWith(prefix, o) && r == 2
            }
        )
    }

    @Test
    fun testEndsWith() {
        check(
            StringExamples::endsWith,
            between(5..6),
            { s, suffix, r -> suffix == null },
            { s, suffix, r -> suffix != null && suffix.length < 2 },
            { s, suffix, r -> suffix != null && suffix.length >= 2 && s == null },
            { s, suffix, r -> suffix != null && suffix.length >= 2 && s != null && s.endsWith(suffix) && r == true },
            { s, suffix, r -> suffix != null && suffix.length >= 2 && s != null && !s.endsWith(suffix) && r == false }
        )
    }

    @Test
    @Disabled("TODO: support replace")
    fun testReplaceAll() {
        checkWithException(
            StringExamples::replaceAll,
            eq(4),
            { s, _, _, r -> s == null && r.isException<NullPointerException>() },
            { s, regex, _, r -> s != null && regex == null && r.isException<NullPointerException>() },
            { s, regex, replacement, r -> s != null && regex != null && replacement == null && r.isException<NullPointerException>() },
            { s, regex, replacement, r ->
                s != null && regex != null && replacement != null && r.getOrThrow() == s.replace(regex, replacement)
            }, // one replace only!
        )
    }

    @Test
    fun testLastIndexOf() {
        check(
            StringExamples::lastIndexOf,
            between(5..7),
            { s, _, _ -> s == null },
            { s, find, _ -> s != null && find == null },
            { s, find, r -> r == s.lastIndexOf(find) && r == s.length - find.length },
            { s, find, r -> r == s.lastIndexOf(find) && r < s.length - find.length },
            { s, find, r -> r == s.lastIndexOf(find) && r == -1 },
        )
    }

    @Test
    fun testIndexOfWithOffset() {
        check(
            StringExamples::indexOfWithOffset,
            between(5..9),
            { s, _, _, _ -> s == null },
            { s, find, _, _ -> s != null && find == null },
            { s, find, offset, r -> r == s.indexOf(find, offset) && r > offset && offset > 0 },
            { s, find, offset, r -> r == s.indexOf(find, offset) && r == offset },
            { s, find, offset, r -> r == s.indexOf(find, offset) && !(r == offset || (offset in 1 until r)) },
        )
    }


    @Test
    fun testLastIndexOfWithOffset() {
        check(
            StringExamples::lastIndexOfWithOffset,
            between(5..9),
            { s, _, _, _ -> s == null },
            { s, find, _, _ -> s != null && find == null },
            { s, find, i, r -> r == s.lastIndexOf(find, i) && r >= 0 && r < i - find.length && i < s.length },
            { s, find, i, r -> r == s.lastIndexOf(find, i) && r >= 0 && !(r < i - find.length && i < s.length) },
            { s, find, i, r -> r == s.lastIndexOf(find, i) && r == -1 },
        )
    }

    @Test
    fun testCompareCodePoints() {
        checkWithException(
            StringExamples::compareCodePoints,
            between(8..10),
            { s, t, i, r -> s == null && r.isException<NullPointerException>() },
            { s, t, i, r -> s != null && i < 0 || i >= s.length && r.isException<StringIndexOutOfBoundsException>() },
            { s, t, i, r -> s != null && t == null && r.isException<NullPointerException>() },
            { s, t, i, r -> t != null && i < 0 || i >= t.length && r.isException<StringIndexOutOfBoundsException>() },
            { s, t, i, r -> s != null && t != null && s.codePointAt(i) < t.codePointAt(i) && i == 0 && r.getOrThrow() == 0 },
            { s, t, i, r -> s != null && t != null && s.codePointAt(i) < t.codePointAt(i) && i != 0 && r.getOrThrow() == 1 },
            { s, t, i, r -> s != null && t != null && s.codePointAt(i) >= t.codePointAt(i) && i == 0 && r.getOrThrow() == 2 },
            { s, t, i, r -> s != null && t != null && s.codePointAt(i) >= t.codePointAt(i) && i != 0 && r.getOrThrow() == 3 },
        )
    }

    @Test
    fun testToCharArray() {
        check(
            StringExamples::toCharArray,
            eq(2),
            { s, r -> s == null },
            { s, r -> s.toCharArray().contentEquals(r) }
        )
    }

    @Test
    fun testGetObj() {
        check(
            StringExamples::getObj,
            eq(1),
            { obj, r -> obj == r }
        )
    }

    @Test
    fun testGetObjWithCondition() {
        check(
            StringExamples::getObjWithCondition,
            between(3..4),
            { obj, r -> obj == null && r == "null" },
            { obj, r -> obj != null && obj == "BEDA" && r == "48858" },
            { obj, r -> obj != null && obj != "BEDA" && obj == r }
        )
    }

    @Test
    fun testEqualsIgnoreCase() {
        withPushingStateFromPathSelectorForConcrete {
            check(
                StringExamples::equalsIgnoreCase,
                eq(2),
                { s, r -> "SUCCESS".equals(s, ignoreCase = true) && r == "success" },
                { s, r -> !"SUCCESS".equals(s, ignoreCase = true) && r == "failure" },
            )
        }
    }

    @Test
    fun testListToString() {
        check(
            StringExamples::listToString,
            eq(1),
            { r -> r == "[a, b, c]"},
            coverage = DoNotCalculate
        )
    }
}
