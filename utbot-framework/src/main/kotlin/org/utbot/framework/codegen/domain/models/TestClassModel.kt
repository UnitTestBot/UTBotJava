package org.utbot.framework.codegen.domain.models

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.mapper.UtModelDeepMapper
import org.utbot.framework.plugin.api.mapper.mapStateBeforeModels

/**
 * Stores method test sets in a structure that replicates structure of their methods in [classUnderTest].
 * I.e., if some method is declared in nested class of [classUnderTest], its testset will be put
 * in [TestClassModel] in one of [nestedClasses]
 */
abstract class TestClassModel(
    val classUnderTest: ClassId,
    val methodTestSets: List<CgMethodTestSet>,
    val nestedClasses: List<SimpleTestClassModel>,
)

class SimpleTestClassModel(
    classUnderTest: ClassId,
    methodTestSets: List<CgMethodTestSet>,
    nestedClasses: List<SimpleTestClassModel> = listOf(),
): TestClassModel(classUnderTest, methodTestSets, nestedClasses)

fun SimpleTestClassModel.mapStateBeforeModels(mapperProvider: () -> UtModelDeepMapper) =
    SimpleTestClassModel(
        classUnderTest = classUnderTest,
        nestedClasses = nestedClasses,
        methodTestSets = methodTestSets.map { testSet ->
            testSet.substituteExecutions(
                testSet.executions.map { execution -> execution.mapStateBeforeModels(mapperProvider()) }
            )
        }
    )
