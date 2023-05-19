package org.utbot.framework.codegen.domain.models

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.plugin.api.ClassId

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

/**
 * Extended [SimpleTestClassModel] for Spring analysis reasons
 *
 * @param injectingMocksClass a class to inject other mocks into
 * @param mockedClasses variables of test class to represent mocked instances
 */
class SpringTestClassModel(
    classUnderTest: ClassId,
    methodTestSets: List<CgMethodTestSet>,
    nestedClasses: List<SimpleTestClassModel>,
    val injectedMockModels: Map<ClassId, Set<UtModelWrapper>> = mapOf(),
    val mockedModels: Map<ClassId, Set<UtModelWrapper>> = mapOf(),
): TestClassModel(classUnderTest, methodTestSets, nestedClasses)

