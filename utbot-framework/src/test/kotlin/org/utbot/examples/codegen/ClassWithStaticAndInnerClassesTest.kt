package org.utbot.examples.codegen

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

@Suppress("INACCESSIBLE_TYPE")
internal class ClassWithStaticAndInnerClassesTest : UtValueTestCaseChecker(testClass = ClassWithStaticAndInnerClasses::class) {
    @Test
    fun testUsePrivateStaticClassWithPrivateField() {
        check(
            ClassWithStaticAndInnerClasses::usePrivateStaticClassWithPrivateField,
            eq(2),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testUsePrivateStaticClassWithPublicField() {
        check(
            ClassWithStaticAndInnerClasses::usePrivateStaticClassWithPublicField,
            eq(2),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testUsePublicStaticClassWithPrivateField() {
        check(
            ClassWithStaticAndInnerClasses::usePublicStaticClassWithPrivateField,
            eq(2),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testUsePublicStaticClassWithPublicField() {
        check(
            ClassWithStaticAndInnerClasses::usePublicStaticClassWithPublicField,
            eq(2),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testUsePrivateInnerClassWithPrivateField() {
        check(
            ClassWithStaticAndInnerClasses::usePrivateInnerClassWithPrivateField,
            eq(2),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testUsePrivateInnerClassWithPublicField() {
        check(
            ClassWithStaticAndInnerClasses::usePrivateInnerClassWithPublicField,
            eq(2),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testUsePublicInnerClassWithPrivateField() {
        check(
            ClassWithStaticAndInnerClasses::usePublicInnerClassWithPrivateField,
            eq(2),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testUsePublicInnerClassWithPublicField() {
        check(
            ClassWithStaticAndInnerClasses::usePublicInnerClassWithPublicField,
            eq(2),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testUsePackagePrivateFinalStaticClassWithPackagePrivateField() {
        check(
            ClassWithStaticAndInnerClasses::usePackagePrivateFinalStaticClassWithPackagePrivateField,
            eq(2),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testUsePackagePrivateFinalInnerClassWithPackagePrivateField() {
        check(
            ClassWithStaticAndInnerClasses::usePackagePrivateFinalInnerClassWithPackagePrivateField,
            eq(2),
            coverage = DoNotCalculate
        )
    }
}