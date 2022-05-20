package org.utbot.framework.codegen.model.constructor.util

import org.utbot.framework.codegen.Junit4
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.TestNg
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.name.CgNameGenerator
import org.utbot.framework.codegen.model.constructor.name.CgNameGeneratorImpl
import org.utbot.framework.codegen.model.constructor.tree.CgCallableAccessManager
import org.utbot.framework.codegen.model.constructor.tree.CgCallableAccessManagerImpl
import org.utbot.framework.codegen.model.constructor.tree.CgMethodConstructor
import org.utbot.framework.codegen.model.constructor.tree.CgTestClassConstructor
import org.utbot.framework.codegen.model.constructor.tree.CgVariableConstructor
import org.utbot.framework.codegen.model.constructor.tree.CgFieldStateManager
import org.utbot.framework.codegen.model.constructor.tree.CgFieldStateManagerImpl
import org.utbot.framework.codegen.model.constructor.tree.Junit4Manager
import org.utbot.framework.codegen.model.constructor.tree.Junit5Manager
import org.utbot.framework.codegen.model.constructor.tree.MockFrameworkManager
import org.utbot.framework.codegen.model.constructor.tree.TestFrameworkManager
import org.utbot.framework.codegen.model.constructor.tree.TestNgManager

// TODO: probably rewrite it to delegates so that we could write 'val testFrameworkManager by CgComponents' etc.
internal object CgComponents {
    fun getNameGeneratorBy(context: CgContext) = nameGenerators.getOrPut(context) { CgNameGeneratorImpl(context) }

    fun getCallableAccessManagerBy(context: CgContext) =
            callableAccessManagers.getOrPut(context) { CgCallableAccessManagerImpl(context) }

    fun getStatementConstructorBy(context: CgContext) =
            statementConstructors.getOrPut(context) { CgStatementConstructorImpl(context) }

    fun getTestFrameworkManagerBy(context: CgContext) = when (context.testFramework) {
        is Junit4 -> testFrameworkManagers.getOrPut(context) { Junit4Manager(context) }
        is Junit5 -> testFrameworkManagers.getOrPut(context) { Junit5Manager(context) }
        is TestNg -> testFrameworkManagers.getOrPut(context) { TestNgManager(context) }
    }

    fun getMockFrameworkManagerBy(context: CgContext) =
            mockFrameworkManagers.getOrPut(context) { MockFrameworkManager(context) }

    fun getFieldStateManagerBy(context: CgContext) =
            fieldStateManagers.getOrPut(context) { CgFieldStateManagerImpl(context) }

    fun getVariableConstructorBy(context: CgContext) = variableConstructors.getOrPut(context) { CgVariableConstructor(context) }

    fun getMethodConstructorBy(context: CgContext) = methodConstructors.getOrPut(context) { CgMethodConstructor(context) }
    fun getTestClassConstructorBy(context: CgContext) = testClassConstructors.getOrPut(context) { CgTestClassConstructor(context) }

    private val nameGenerators: MutableMap<CgContext, CgNameGenerator> = mutableMapOf()
    private val statementConstructors: MutableMap<CgContext, CgStatementConstructor> = mutableMapOf()
    private val callableAccessManagers: MutableMap<CgContext, CgCallableAccessManager> = mutableMapOf()
    private val testFrameworkManagers: MutableMap<CgContext, TestFrameworkManager> = mutableMapOf()
    private val mockFrameworkManagers: MutableMap<CgContext, MockFrameworkManager> = mutableMapOf()
    private val fieldStateManagers: MutableMap<CgContext, CgFieldStateManager> = mutableMapOf()

    private val variableConstructors: MutableMap<CgContext, CgVariableConstructor> = mutableMapOf()
    private val methodConstructors: MutableMap<CgContext, CgMethodConstructor> = mutableMapOf()
    private val testClassConstructors: MutableMap<CgContext, CgTestClassConstructor> = mutableMapOf()
}