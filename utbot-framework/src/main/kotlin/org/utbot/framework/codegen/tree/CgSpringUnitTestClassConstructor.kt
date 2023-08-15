package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.builtin.closeMethodId
import org.utbot.framework.codegen.domain.builtin.openMocksMethodId
import org.utbot.framework.codegen.domain.builtin.clearMethodId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgAssignment
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.CgStatementExecutableCall
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.domain.models.SimpleTestClassModel
import org.utbot.framework.codegen.tree.fieldmanager.CgInjectingMocksFieldsManager
import org.utbot.framework.codegen.tree.fieldmanager.CgMockedFieldsManager
import org.utbot.framework.codegen.tree.fieldmanager.CgSpiedFieldsManager
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectClassId

class CgSpringUnitTestClassConstructor(context: CgContext) : CgAbstractSpringTestClassConstructor(context) {

    private var additionalMethodsRequired: Boolean = false
    private lateinit var mockitoCloseableVariable: CgValue
    private lateinit var spyClearVariables: List<CgValue>

    private val mocksFieldsManager = CgMockedFieldsManager(context)
    private val spiesFieldsManager = CgSpiedFieldsManager(context)
    private val injectingMocksFieldsManager =
        CgInjectingMocksFieldsManager(context, mocksFieldsManager, spiesFieldsManager)

    override fun constructClassFields(testClassModel: SimpleTestClassModel): List<CgFieldDeclaration> {
        val fields = mutableListOf<CgFieldDeclaration>()

        val spiesFields = spiesFieldsManager.createFieldDeclarations(testClassModel)
        val mockedFields = mocksFieldsManager.createFieldDeclarations(testClassModel)

        if ((spiesFields + mockedFields).isNotEmpty()) {
            val injectingMocksFields = injectingMocksFieldsManager.createFieldDeclarations(testClassModel)

            fields += injectingMocksFields
            fields += mockedFields
            fields += spiesFields
            fields += constructMockitoCloseables()

            additionalMethodsRequired = true
            spyClearVariables = spiesFields.map { it.declaration.variable }
        }

        return fields
    }

    override fun constructAdditionalUtilMethods(): CgMethodsCluster? {
        if (!additionalMethodsRequired) return null

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

        val clearSpyModelCalls = spyClearVariables.map { spyVariable ->
            CgMethodCall(
                caller = spyVariable,
                executableId = clearMethodId(spyVariable.type.jClass),
                arguments = emptyList()
            )
        }

        val openMocksStatement = CgAssignment(mockitoCloseableVariable, openMocksCall)
        val closeStatement = CgStatementExecutableCall(closeCall)
        val clearSpyModels = clearSpyModelCalls.map { CgStatementExecutableCall(it) }

        return CgMethodsCluster.withoutDocs(
            listOf(
                constructBeforeMethod(listOf(openMocksStatement)),
                constructAfterMethod(clearSpyModels + listOf(closeStatement)),
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