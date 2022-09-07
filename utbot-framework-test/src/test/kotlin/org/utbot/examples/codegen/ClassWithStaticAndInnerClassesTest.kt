package org.utbot.examples.codegen

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

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

    @Test
    fun testGetValueFromPublicFieldWithPrivateType() {
        check(
            ClassWithStaticAndInnerClasses::getValueFromPublicFieldWithPrivateType,
            eq(2),
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testPublicStaticClassWithPrivateField_DeepNestedStatic_g() {
        checkAllCombinations(
            ClassWithStaticAndInnerClasses.PublicStaticClassWithPrivateField.DeepNestedStatic::g,
            generateWithNested = true
        )
    }

    @Test
    fun testPublicStaticClassWithPrivateField_DeepNested_h() {
        checkAllCombinations(
            ClassWithStaticAndInnerClasses.PublicStaticClassWithPrivateField.DeepNested::h,
            generateWithNested = true
        )
    }
}