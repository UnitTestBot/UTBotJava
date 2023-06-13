package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgClass
import org.utbot.framework.codegen.domain.models.CgEnumConstantAccess
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.SpringTestClassModel
import org.utbot.framework.plugin.api.UtSpringContextModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.autowiredClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.dirtiesContextClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.dirtiesContextClassModeClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.springBootTestClassId

class CgSpringIntegrationTestClassConstructor(context: CgContext) : CgAbstractSpringTestClassConstructor(context) {
    override fun constructTestClass(testClassModel: SpringTestClassModel): CgClass {
        return buildClass {
            id = currentTestClass

            body = constructTestClassBody(testClassModel)

            with (currentTestClassContext) {
                annotations += statementConstructor.annotation(springBootTestClassId)
                annotations += statementConstructor.annotation(
                    classId = dirtiesContextClassId,
                    namedArguments = listOf(
                        "classMode" to CgEnumConstantAccess(dirtiesContextClassModeClassId, "BEFORE_EACH_TEST_METHOD")
                    )
                )
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

    override fun constructAdditionalMethods() = CgMethodsCluster(
        header = null,
        content = emptyList(),
    )
}