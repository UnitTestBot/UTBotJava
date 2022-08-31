package org.utbot.framework.codegen.model.constructor.util

import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.name.CgNameGenerator
import org.utbot.framework.codegen.model.constructor.name.CgNameGeneratorImpl
import org.utbot.framework.codegen.model.constructor.name.PythonCgNameGenerator
import org.utbot.framework.codegen.model.constructor.tree.*
import org.utbot.framework.codegen.model.constructor.tree.CgCallableAccessManagerImpl
import org.utbot.framework.codegen.model.constructor.tree.CgFieldStateManager
import org.utbot.framework.codegen.model.constructor.tree.CgFieldStateManagerImpl
import org.utbot.framework.codegen.model.constructor.tree.CgMethodConstructor
import org.utbot.framework.codegen.model.constructor.tree.CgTestClassConstructor
import org.utbot.framework.codegen.model.constructor.tree.CgVariableConstructor
import org.utbot.framework.codegen.model.constructor.tree.Junit4Manager
import org.utbot.framework.codegen.model.constructor.tree.Junit5Manager
import org.utbot.framework.codegen.model.constructor.tree.MockFrameworkManager
import org.utbot.framework.codegen.model.constructor.tree.PytestManager
import org.utbot.framework.codegen.model.constructor.tree.TestFrameworkManager
import org.utbot.framework.codegen.model.constructor.tree.TestNgManager
import org.utbot.framework.plugin.api.CodegenLanguage

// TODO: probably rewrite it to delegates so that we could write 'val testFrameworkManager by CgComponents' etc.
internal object CgComponents {
    fun getNameGeneratorBy(context: CgContext) = nameGenerators.getOrPut(context) {
        when(context.testFramework) {
            is Pytest -> PythonCgNameGenerator(context)
            is Unittest -> PythonCgNameGenerator(context)
            else -> CgNameGeneratorImpl(context)
        }
    }

    fun getCallableAccessManagerBy(context: CgContext) =
        callableAccessManagers.getOrPut(context) {
            when (context.testFramework) {
                is Pytest -> PythonCgCallableAccessManagerImpl(context)
                is Unittest -> PythonCgCallableAccessManagerImpl(context)
                else -> CgCallableAccessManagerImpl(context)
            }
        }

    fun getStatementConstructorBy(context: CgContext) =
        statementConstructors.getOrPut(context) {
            when (context.testFramework) {
                is Pytest -> PythonCgStatementConstructorImpl(context)
                is Unittest -> PythonCgStatementConstructorImpl(context)
                else -> CgStatementConstructorImpl(context)
            }
        }

    fun getTestFrameworkManagerBy(context: CgContext) = when (context.testFramework) {
        is Junit4 -> testFrameworkManagers.getOrPut(context) { Junit4Manager(context) }
        is Junit5 -> testFrameworkManagers.getOrPut(context) { Junit5Manager(context) }
        is TestNg -> testFrameworkManagers.getOrPut(context) { TestNgManager(context) }
        is Pytest -> testFrameworkManagers.getOrPut(context) { PytestManager(context) }
        is Unittest -> testFrameworkManagers.getOrPut(context) { UnittestManager(context) }
    }

    fun getMockFrameworkManagerBy(context: CgContext) =
            mockFrameworkManagers.getOrPut(context) { MockFrameworkManager(context) }

    fun getFieldStateManagerBy(context: CgContext) =
            fieldStateManagers.getOrPut(context) { CgFieldStateManagerImpl(context) }

    fun getVariableConstructorBy(context: CgContext) = variableConstructors.getOrPut(context) {
        when (context.codegenLanguage) {
            CodegenLanguage.PYTHON -> PythonCgVariableConstructor(context)
            else -> CgVariableConstructor(context)
        }
    }

    fun getMethodConstructorBy(context: CgContext) = methodConstructors.getOrPut(context) {
        when (context.codegenLanguage) {
            CodegenLanguage.PYTHON -> PythonCgMethodConstructor(context)
            else -> CgMethodConstructor(context)
        }
    }

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