package org.utbot.framework.codegen.domain.models.builders

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.SimpleTestClassModel
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.enclosingClass

open class SimpleTestClassModelBuilder(context: CgContext): TestClassModelBuilder() {
    override fun createTestClassModel(
        classUnderTest: ClassId,
        testSets: List<CgMethodTestSet>,
    ): SimpleTestClassModel {
        // For each class stores list of methods declared in this class (methods from nested classes are excluded)
        val class2methodTestSets = testSets.groupBy { it.executableId.classId }

        val classesWithMethodsUnderTest = testSets
            .map { it.executableId.classId }
            .distinct()

        // For each class stores list of its "direct" nested classes
        val class2nestedClasses = mutableMapOf<ClassId, MutableSet<ClassId>>()

        for (classId in classesWithMethodsUnderTest) {
            var currentClass = classId
            var enclosingClass = currentClass.enclosingClass
            // while we haven't reached the top of nested class hierarchy or the main class under test
            while (enclosingClass != null && currentClass != classUnderTest) {
                class2nestedClasses.getOrPut(enclosingClass) { mutableSetOf() } += currentClass
                currentClass = enclosingClass
                enclosingClass = enclosingClass.enclosingClass
            }
        }

        return constructRecursively(classUnderTest, class2methodTestSets, class2nestedClasses)
    }

    private fun constructRecursively(
        clazz: ClassId,
        class2methodTestSets: Map<ClassId, List<CgMethodTestSet>>,
        class2nestedClasses: Map<ClassId, Set<ClassId>>
    ): SimpleTestClassModel {
        val currentNestedClasses = class2nestedClasses.getOrDefault(clazz, listOf())
        val currentMethodTestSets = class2methodTestSets.getOrDefault(clazz, listOf())
        return SimpleTestClassModel(
            clazz,
            currentMethodTestSets,
            currentNestedClasses.map {
                constructRecursively(it, class2methodTestSets, class2nestedClasses)
            }
        )
    }
}