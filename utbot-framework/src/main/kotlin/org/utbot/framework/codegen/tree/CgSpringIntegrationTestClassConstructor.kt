package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.builtin.autowiredClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.SpringTestClassModel
import org.utbot.framework.plugin.api.UtSpringContextModel

class CgSpringIntegrationTestClassConstructor(context: CgContext) : CgAbstractSpringTestClassConstructor(context) {

    override fun constructClassFields(testClassModel: SpringTestClassModel): List<CgFieldDeclaration> {
        // NOTE that it is important to construct applicationContext variable
        // even though it is not used in generated code
        // because it is used in some assemble models as an instance of instantiation call.
        variableConstructor.getOrCreateVariable(UtSpringContextModel)

        val autowiredFromContextModels = testClassModel.springSpecificInformation.autowiredFromContextModels
        return constructFieldsWithAnnotation(autowiredClassId, autowiredFromContextModels)
    }

    override fun constructAdditionalMethods() = CgMethodsCluster(
        header = null,
        content = emptyList(),
    )
}