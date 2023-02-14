package org.utbot.framework.codegen.domain.models

import org.utbot.framework.plugin.api.ClassId

// TODO: seems like this class needs to be renamed
/**
 * Stores method test sets in a structure that replicates structure of their methods in [classUnderTest].
 * I.e., if some method is declared in nested class of [classUnderTest], its testset will be put
 * in [TestClassModel] in one of [nestedClasses]
 *
 * @param injectingMocksClass a class to inject other mocks into
 * @param mockedClasses variables of test class to represent mocked instances
 */
class TestClassModel(
    val classUnderTest: ClassId,
    val methodTestSets: List<CgMethodTestSet>,
    val nestedClasses: List<TestClassModel> = listOf(),
    val injectingMocksClass: ClassId? = null,
    val mockedClasses: Set<ClassId> = setOf(),
)