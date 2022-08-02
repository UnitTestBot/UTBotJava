package org.utbot.framework.codegen.model.constructor

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.enclosingClass

// TODO: seems like this class needs to be renamed
/**
 * Stores method testsets in a structure that replicates structure of their methods in [classUnderTest].
 * I.e., if some method is declared in nested class of [classUnderTest], its testset will be put
 * in [UtTestClass] in one of [nestedClasses]
 */
data class UtTestClass(
    val classUnderTest: ClassId,
    val methodTestSets: List<CgMethodTestSet>,
    val nestedClasses: List<UtTestClass> = listOf()
) {
    companion object {
        fun fromTestSets(classUnderTest: ClassId, testSets: List<CgMethodTestSet>): UtTestClass {
            val class2methodTestSets = testSets.groupBy { it.executableId.classId }
            val class2nestedClasses = testSets
                .map { it.executableId.classId }
                .flatMap { generateSequence(it) { it.enclosingClass }.takeWhile { it != classUnderTest } }
                .groupBy { it.enclosingClass ?: error("All of the given to codegen methods should belong to classUnderTest") }
                .mapValues { (_, v) -> v.distinct() }
            return constructRecursively(classUnderTest, class2methodTestSets, class2nestedClasses)
        }

        private fun constructRecursively(
            clazz: ClassId,
            class2methodTestSets: Map<ClassId, List<CgMethodTestSet>>,
            class2nestedClasses: Map<ClassId, List<ClassId>>
        ): UtTestClass {
            val currentNestedClasses = class2nestedClasses.getOrDefault(clazz, listOf())
            val currentMethodTestSets = class2methodTestSets.getOrDefault(clazz, listOf())
            return UtTestClass(
                clazz,
                currentMethodTestSets,
                currentNestedClasses.map {
                    constructRecursively(it, class2methodTestSets, class2nestedClasses)
                }
            )
        }
    }
}