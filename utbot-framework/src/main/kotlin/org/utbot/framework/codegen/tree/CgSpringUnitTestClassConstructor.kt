package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.builtin.closeMethodId
import org.utbot.framework.codegen.domain.builtin.injectMocksClassId
import org.utbot.framework.codegen.domain.builtin.mockClassId
import org.utbot.framework.codegen.domain.builtin.openMocksMethodId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgAssignment
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.CgSimpleRegion
import org.utbot.framework.codegen.domain.models.CgStatementExecutableCall
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.domain.models.SpringTestClassModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectClassId

class CgSpringUnitTestClassConstructor(context: CgContext) : CgAbstractSpringTestClassConstructor(context) {

    private lateinit var mockitoCloseableVariable: CgValue

    override fun constructClassFields(testClassModel: SpringTestClassModel): List<CgFieldDeclaration> {
        val fields = mutableListOf<CgFieldDeclaration>()
        val mockedFields = constructFieldsWithAnnotation(testClassModel.thisInstanceDependentMocks, mockClassId)

        if (mockedFields.isNotEmpty()) {
            fields += constructFieldsWithAnnotation(testClassModel.thisInstanceModels, injectMocksClassId)
            fields += mockedFields

            fields += constructMockitoCloseables()
        }

        return fields
    }

    override fun constructAdditionalMethods(): CgMethodsCluster {
        importIfNeeded(openMocksMethodId)

        val openMocksCall = CgMethodCall(
            caller = null,
            executableId = openMocksMethodId,
            //TODO: this is a hack of this
            arguments = listOf(CgVariable("this", objectClassId))
        )

        val closeCall = CgMethodCall(
            caller = mockitoCloseableVariable,
            executableId = closeMethodId,
            arguments = emptyList(),
        )

        val openMocksStatement = CgAssignment(mockitoCloseableVariable, openMocksCall)
        val closeStatement = CgStatementExecutableCall(closeCall)

        return CgMethodsCluster(
            header = null,
            listOf(
                CgSimpleRegion(
                    header = null,
                    listOf(
                        constructBeforeMethod(listOf(openMocksStatement)),
                        constructAfterMethod(listOf(closeStatement)),
                    )
                )
            )
        )
    }

    private fun constructMockitoCloseables(): CgFieldDeclaration {
        val mockitoCloseableVarName = "mockitoCloseable"
        val mockitoCloseableVarType = java.lang.AutoCloseable::class.id

        val mockitoCloseableModel = UtCompositeModel(
            id = null,
            classId = mockitoCloseableVarType,
            isMock = false,
        )

        mockitoCloseableVariable =
            variableConstructor.getOrCreateVariable(mockitoCloseableModel, mockitoCloseableVarName)
        val mockitoCloseableDeclaration = CgDeclaration(mockitoCloseableVarType, mockitoCloseableVarName, initializer = null)
        return CgFieldDeclaration(ownerClassId = currentTestClass, mockitoCloseableDeclaration)
    }
}