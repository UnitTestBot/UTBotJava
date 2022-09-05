package org.utbot.examples.stdlib

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withUsingReflectionForMaximizingCoverage
import java.util.Date

@Disabled("Java 11 transition -- these tests seems to take too much time and memory")
class DateExampleTest : UtValueTestCaseChecker(testClass = DateExample::class) {
    @Suppress("SpellCheckingInspection")
    @Tag("slow")
    @Test
    fun testGetTimeWithNpeChecksForNonPublicFields() {
        withUsingReflectionForMaximizingCoverage(maximizeCoverage = true) {
            checkWithException(
                DateExample::getTime,
                eq(5),
                *commonMatchers,
                { date: Date?, r: Result<Boolean> ->
                    val cdate = date!!.getDeclaredFieldValue("cdate")
                    val calendarDate = cdate!!.getDeclaredFieldValue("date")

                    calendarDate == null && r.isException<NullPointerException>()
                },
                { date: Date?, r: Result<Boolean> ->
                    val cdate = date!!.getDeclaredFieldValue("cdate")
                    val calendarDate = cdate!!.getDeclaredFieldValue("date")

                    val gcal = date.getDeclaredFieldValue("gcal")

                    val normalized = calendarDate!!.getDeclaredFieldValue("normalized") as Boolean
                    val gregorianYear = calendarDate.getDeclaredFieldValue("gregorianYear") as Int

                    gcal == null && !normalized && gregorianYear >= 1582 && r.isException<NullPointerException>()
                }
            )
        }
    }

    @Test
    fun testGetTimeWithoutReflection() {
        withUsingReflectionForMaximizingCoverage(maximizeCoverage = false) {
            checkWithException(
                DateExample::getTime,
                eq(3),
                *commonMatchers
            )
        }
    }

    private val commonMatchers = arrayOf(
        { date: Date?, r: Result<Boolean> -> date == null && r.isException<NullPointerException>() },
        { date: Date?, r: Result<Boolean> -> date != null && date.time == 100L && r.getOrThrow() },
        { date: Date?, r: Result<Boolean> -> date != null && date.time != 100L && !r.getOrThrow() }
    )

    private fun Any.getDeclaredFieldValue(fieldName: String): Any? {
        val declaredField = javaClass.getDeclaredField(fieldName)
        declaredField.isAccessible = true

        return declaredField.get(this)
    }
}