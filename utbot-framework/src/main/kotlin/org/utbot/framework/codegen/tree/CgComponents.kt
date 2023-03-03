package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.services.CgNameGenerator
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.services.framework.MockFrameworkManager
import org.utbot.framework.codegen.services.framework.TestFrameworkManager

object CgComponents {

    /**
     * Clears all stored data for current [CgContext].
     * As far as context is created per class under test,
     * no related data is required after it's processing.
     */
    fun clearContextRelatedStorage() {
        nameGenerators.clear()
        statementConstructors.clear()
        callableAccessManagers.clear()
        testFrameworkManagers.clear()
        mockFrameworkManagers.clear()
        variableConstructors.clear()
        methodConstructors.clear()
    }

    private val nameGenerators: MutableMap<CgContext, CgNameGenerator> = mutableMapOf()
    private val statementConstructors: MutableMap<CgContext, CgStatementConstructor> = mutableMapOf()
    private val callableAccessManagers: MutableMap<CgContext, CgCallableAccessManager> = mutableMapOf()
    private val testFrameworkManagers: MutableMap<CgContext, TestFrameworkManager> = mutableMapOf()
    private val mockFrameworkManagers: MutableMap<CgContext, MockFrameworkManager> = mutableMapOf()

    private val variableConstructors: MutableMap<CgContext, CgVariableConstructor> = mutableMapOf()
    private val methodConstructors: MutableMap<CgContext, CgMethodConstructor> = mutableMapOf()

    fun getNameGeneratorBy(context: CgContext) = nameGenerators.getOrPut(context) {
        context.cgLanguageAssistant.getNameGeneratorBy(context)
    }
    fun getCallableAccessManagerBy(context: CgContext) = callableAccessManagers.getOrPut(context) {
        context.cgLanguageAssistant.getCallableAccessManagerBy(context)
    }
    fun getStatementConstructorBy(context: CgContext) = statementConstructors.getOrPut(context) {
        context.cgLanguageAssistant.getStatementConstructorBy(context)
    }

    fun getTestFrameworkManagerBy(context: CgContext) =
        testFrameworkManagers.getOrDefault(context, context.cgLanguageAssistant.getLanguageTestFrameworkManager().managerByFramework(context))

    fun getMockFrameworkManagerBy(context: CgContext) = mockFrameworkManagers.getOrPut(context) { MockFrameworkManager(context) }
    fun getVariableConstructorBy(context: CgContext) = variableConstructors.getOrPut(context) {
        context.cgLanguageAssistant.getVariableConstructorBy(context)
    }
    fun getMethodConstructorBy(context: CgContext) = methodConstructors.getOrPut(context) {
        context.cgLanguageAssistant.getMethodConstructorBy(context)
    }
}