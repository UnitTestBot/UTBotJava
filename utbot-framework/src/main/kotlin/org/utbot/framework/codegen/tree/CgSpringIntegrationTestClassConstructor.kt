package org.utbot.framework.codegen.tree

import org.utbot.common.tryLoadClass
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.*
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.autoConfigureTestDbClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.autowiredClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.dirtiesContextClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.dirtiesContextClassModeClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.springBootTestClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.transactionalClassId
import org.utbot.framework.plugin.api.util.utContext

class CgSpringIntegrationTestClassConstructor(context: CgContext) : CgAbstractSpringTestClassConstructor(context) {
    override fun constructTestClass(testClassModel: SpringTestClassModel): CgClass {
        return buildClass {
            id = currentTestClass

            body = constructTestClassBody(testClassModel)

            with (currentTestClassContext) {
                annotations += collectSpringSpecificAnnotations()
                annotations += collectedTestClassAnnotations

                superclass = testClassSuperclass
                interfaces += collectedTestClassInterfaces
            }
        }
    }

    override fun constructClassFields(testClassModel: SpringTestClassModel): List<CgFieldDeclaration> {
        val autowiredFromContextModels = testClassModel.springSpecificInformation.autowiredFromContextModels
        return constructFieldsWithAnnotation(autowiredClassId, autowiredFromContextModels)
    }

    override fun constructAdditionalMethods() = CgMethodsCluster(header = null, content = emptyList())

    private fun collectSpringSpecificAnnotations(): List<CgAnnotation> {
        val annotations = mutableListOf<CgAnnotation>()

        annotations += statementConstructor.annotation(springBootTestClassId)
        annotations += statementConstructor.annotation(
            classId = dirtiesContextClassId,
            namedArguments = listOf(
                "classMode" to CgEnumConstantAccess(dirtiesContextClassModeClassId, "BEFORE_EACH_TEST_METHOD")
            )
        )

        listOf(transactionalClassId, autoConfigureTestDbClassId)
            .filter { annotationTypeIsAccessible(it) }
            .forEach { annotations += statementConstructor.annotation(it) }

        return annotations
    }

    private fun annotationTypeIsAccessible(annotationType: ClassId): Boolean =
        utContext.classLoader.tryLoadClass(annotationType.name) != null
}