package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.services.CgNameGenerator
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.services.framework.MockFrameworkManager
import org.utbot.framework.codegen.services.framework.TestFrameworkManager
import java.util.*

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

    private val nameGenerators: IdentityHashMap<CgContext, CgNameGenerator> = IdentityHashMap()

    private val callableAccessManagers: IdentityHashMap<CgContext, CgCallableAccessManager> = IdentityHashMap()
    private val testFrameworkManagers: IdentityHashMap<CgContext, TestFrameworkManager> = IdentityHashMap()
    private val mockFrameworkManagers: IdentityHashMap<CgContext, MockFrameworkManager> = IdentityHashMap()

    private val statementConstructors: IdentityHashMap<CgContext, CgStatementConstructor> = IdentityHashMap()
    private val variableConstructors: IdentityHashMap<CgContext, CgVariableConstructor> = IdentityHashMap()
    private val methodConstructors: IdentityHashMap<CgContext, CgMethodConstructor> = IdentityHashMap()

    fun getNameGeneratorBy(context: CgContext): CgNameGenerator = nameGenerators.getOrPut(context) {
        context.cgLanguageAssistant.getNameGeneratorBy(context)
    }

    fun getCallableAccessManagerBy(context: CgContext): CgCallableAccessManager = callableAccessManagers.getOrPut(context) {
        context.cgLanguageAssistant.getCallableAccessManagerBy(context)
    }

    fun getStatementConstructorBy(context: CgContext): CgStatementConstructor = statementConstructors.getOrPut(context) {
        context.cgLanguageAssistant.getStatementConstructorBy(context)
    }

    fun getTestFrameworkManagerBy(context: CgContext): TestFrameworkManager =
        testFrameworkManagers.getOrDefault(
            context,
            context.cgLanguageAssistant.getLanguageTestFrameworkManager().managerByFramework(context)
        )

    fun getMockFrameworkManagerBy(context: CgContext): MockFrameworkManager =
        mockFrameworkManagers.getOrPut(context) { MockFrameworkManager(context) }

    fun getVariableConstructorBy(context: CgContext): CgVariableConstructor = variableConstructors.getOrPut(context) {
        context.cgLanguageAssistant.getVariableConstructorBy(context)
    }

    fun getMethodConstructorBy(context: CgContext): CgMethodConstructor = methodConstructors.getOrPut(context) {
        context.cgLanguageAssistant.getMethodConstructorBy(context)
    }
}