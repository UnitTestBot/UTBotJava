package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.builtin.autowiredClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.SpringTestClassModel

class CgSpringIntegrationTestClassConstructor(context: CgContext) : CgAbstractSpringTestClassConstructor(context) {

    override fun constructClassFields(testClassModel: SpringTestClassModel): List<CgFieldDeclaration> {
        val applicationContextModels = testClassModel.springSpecificInformation.applicationContextModels
        val autowiredFromContextModels = testClassModel.springSpecificInformation.autowiredFromContextModels

        // NOTE that it is important to construct applicationContext variable
        // before other class variables that will get beans from it.
        val fields = mutableListOf<CgFieldDeclaration>()
        fields += constructFieldsWithAnnotation(autowiredClassId, applicationContextModels)
        fields += constructFieldsWithAnnotation(autowiredClassId, autowiredFromContextModels)

        return fields
    }

    override fun constructAdditionalMethods() = CgMethodsCluster(
        header = null,
        content = emptyList(),
    )
}