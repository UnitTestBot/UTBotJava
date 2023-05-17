package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.builtin.autowiredClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.CgSimpleRegion
import org.utbot.framework.codegen.domain.models.SpringTestClassModel

class CgSpringIntegrationTestClassConstructor(context: CgContext) :
    CgAbstractSpringTestClassConstructor(context) {

    override fun constructClassFields(testClassModel: SpringTestClassModel): Set<CgFieldDeclaration> {
        return constructFieldsWithAnnotation(testClassModel.thisInstanceModels, autowiredClassId)
    }

    override fun constructAdditionalMethods() = CgMethodsCluster(
        header = null,
        listOf(
            CgSimpleRegion(
                header = null,
                listOf(
                    constructBeforeMethod(emptyList()),
                    constructAfterMethod(emptyList()),
                )
            )
        )
    )
}