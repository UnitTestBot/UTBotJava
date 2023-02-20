package org.utbot.framework.codegen.domain.models.builders

import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.plugin.api.ClassId

abstract class TestClassModelBuilder {
    abstract fun createTestClassModel(classUnderTest: ClassId, testSets: List<CgMethodTestSet>): TestClassModel
}