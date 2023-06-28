package org.utbot.framework.codegen.tree

import org.utbot.common.tryLoadClass
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.*
import org.utbot.framework.codegen.domain.models.AnnotationTarget.*
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.autoConfigureTestDbClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.autowiredClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.bootstrapWithClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.dirtiesContextClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.dirtiesContextClassModeClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.springBootTestContextBootstrapper
import org.utbot.framework.plugin.api.util.SpringModelUtils.transactionalClassId
import org.utbot.framework.plugin.api.util.utContext

class CgSpringIntegrationTestClassConstructor(context: CgContext) : CgAbstractSpringTestClassConstructor(context) {
    override fun constructTestClass(testClassModel: SpringTestClassModel): CgClass {
        collectSpringSpecificAnnotations()
        return super.constructTestClass(testClassModel)
    }

    override fun constructClassFields(testClassModel: SpringTestClassModel): List<CgFieldDeclaration> {
        val autowiredFromContextModels = testClassModel.springSpecificInformation.autowiredFromContextModels
        return constructFieldsWithAnnotation(autowiredClassId, autowiredFromContextModels)
    }

    override fun constructAdditionalMethods() = CgMethodsCluster(header = null, content = emptyList())

    private fun collectSpringSpecificAnnotations() {
        testFrameworkManager.addAnnotationForSpringRunner()
        if (annotationTypeIsAccessible(bootstrapWithClassId)) {
            addAnnotation(bootstrapWithClassId, createGetClassExpression(springBootTestContextBootstrapper), Class)
        }

        addAnnotation(
            classId = dirtiesContextClassId,
            namedArguments = listOf(
                CgNamedAnnotationArgument(
                    name = "classMode",
                    value = CgEnumConstantAccess(dirtiesContextClassModeClassId, "BEFORE_EACH_TEST_METHOD")
                ),
            ),
            target = Class,
        )

        listOf(transactionalClassId, autoConfigureTestDbClassId)
            .filter { annotationTypeIsAccessible(it) }
            .forEach { annType -> addAnnotation(annType, Class) }
    }

    private fun annotationTypeIsAccessible(annotationType: ClassId): Boolean =
        utContext.classLoader.tryLoadClass(annotationType.name) != null
}