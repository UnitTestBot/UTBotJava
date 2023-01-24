package org.utbot.framework.codegen.services.framework

import org.utbot.framework.codegen.domain.MockitoStaticMocking
import org.utbot.framework.codegen.domain.NoStaticMocking
import org.utbot.framework.codegen.domain.builtin.any
import org.utbot.framework.codegen.domain.builtin.anyBoolean
import org.utbot.framework.codegen.domain.builtin.anyByte
import org.utbot.framework.codegen.domain.builtin.anyChar
import org.utbot.framework.codegen.domain.builtin.anyDouble
import org.utbot.framework.codegen.domain.builtin.anyFloat
import org.utbot.framework.codegen.domain.builtin.anyInt
import org.utbot.framework.codegen.domain.builtin.anyLong
import org.utbot.framework.codegen.domain.builtin.anyOfClass
import org.utbot.framework.codegen.domain.builtin.anyShort
import org.utbot.framework.codegen.domain.builtin.argumentMatchersClassId
import org.utbot.framework.codegen.domain.builtin.mockMethodId
import org.utbot.framework.codegen.domain.builtin.mockedConstructionContextClassId
import org.utbot.framework.codegen.domain.builtin.mockitoClassId
import org.utbot.framework.codegen.domain.builtin.thenReturnMethodId
import org.utbot.framework.codegen.domain.builtin.whenMethodId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgAnnotation
import org.utbot.framework.codegen.domain.models.CgAnonymousFunction
import org.utbot.framework.codegen.domain.models.CgAssignment
import org.utbot.framework.codegen.domain.models.CgClass
import org.utbot.framework.codegen.domain.models.CgClassBody
import org.utbot.framework.codegen.domain.models.CgConstructorCall
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgDocumentationComment
import org.utbot.framework.codegen.domain.models.CgExecutableCall
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.CgMockMethod
import org.utbot.framework.codegen.domain.models.CgParameterDeclaration
import org.utbot.framework.codegen.domain.models.CgRunnable
import org.utbot.framework.codegen.domain.models.CgSimpleRegion
import org.utbot.framework.codegen.domain.models.CgStatement
import org.utbot.framework.codegen.domain.models.CgStatementExecutableCall
import org.utbot.framework.codegen.domain.models.CgStaticRunnable
import org.utbot.framework.codegen.domain.models.CgSwitchCase
import org.utbot.framework.codegen.domain.models.CgSwitchCaseLabel
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.services.access.CgCallableAccessManagerImpl
import org.utbot.framework.codegen.tree.*
import org.utbot.framework.codegen.tree.buildClassBody
import org.utbot.framework.codegen.tree.CgStatementConstructor
import org.utbot.framework.codegen.tree.CgVariableConstructor
import org.utbot.framework.codegen.tree.CgStatementConstructorImpl
import org.utbot.framework.codegen.tree.CgTestClassConstructor.CgComponents.getVariableConstructorBy
import org.utbot.framework.codegen.tree.hasAmbiguousOverloadsOf
import org.utbot.framework.codegen.util.isAccessibleFrom
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.TypeParameters
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.util.atomicIntegerClassId
import org.utbot.framework.plugin.api.util.atomicIntegerGet
import org.utbot.framework.plugin.api.util.atomicIntegerGetAndIncrement
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.framework.plugin.api.util.isRefType
import org.utbot.framework.plugin.api.util.exceptions

abstract class CgVariableConstructorComponent(val context: CgContext) :
        CgContextOwner by context,
        CgCallableAccessManager by CgCallableAccessManagerImpl(context),
        CgStatementConstructor by CgStatementConstructorImpl(context) {

    val variableConstructor: CgVariableConstructor by lazy { getVariableConstructorBy(context) }

    fun mockitoArgumentMatchersFor(executable: ExecutableId): Array<CgMethodCall> =
            executable.parameters.map {
                val matcher = it.mockitoAnyMatcher(executable.classId.hasAmbiguousOverloadsOf(executable))
                if (matcher != anyOfClass) argumentMatchersClassId[matcher]() else matchByClass(it)
            }.toTypedArray()

    /**
     * Clears all resources required for [currentExecution].
     */
    open fun clearExecutionResources() {
        // do nothing by default
    }

    // TODO: implement other similar methods like thenThrow, etc.
    fun CgMethodCall.thenReturn(returnType: ClassId, vararg args: CgValue) {
        val castedArgs = args
            // guard args to reuse typecast creation logic
            .map { if (it.type == returnType) it else guardExpression(returnType, it).expression }
            .toTypedArray()

        +this[thenReturnMethodId](*castedArgs)
    }

    fun ClassId.mockitoAnyMatcher(withExplicitClass: Boolean): MethodId =
            when (this) {
                byteClassId -> anyByte
                charClassId -> anyChar
                shortClassId -> anyShort
                intClassId -> anyInt
                longClassId -> anyLong
                floatClassId -> anyFloat
                doubleClassId -> anyDouble
                booleanClassId -> anyBoolean
                // we cannot match by string here
                // because anyString accepts only non-nullable strings but argument could be null
                else -> if (withExplicitClass) anyOfClass else any
            }

    private fun matchByClass(id: ClassId): CgMethodCall =
            argumentMatchersClassId[anyOfClass](getClassOf(id))
}

/**
 * Experimental in-place mocker
 */
private class InPlaceMocker(context: CgContext) : ObjectMocker(context) {

    private var mockID = 0
    override fun createMock(model: UtCompositeModel, baseName: String): CgVariable {
        val className = "${model.classId.simpleName}Mock$mockID"
        val mockClassId =
            BuiltinClassId(
                canonicalName = className,
                simpleName = className,
                isPublic = false
            )
        val methods =
            model.mocks.entries.mapIndexed { modelIndex, (methodId, models) ->
                CgMockMethod(
                    name = methodId.name,
                    returnType = methodId.returnType,
                    parameters = methodId.parameters.mapIndexed { i, param ->
                        CgParameterDeclaration(
                            parameter = declareParameter(
                                type = param,
                                name = "x$i",
                            ),
                            isReferenceType = param.isRefType
                        )
                    },
                    statements = block {
                        withNameScope {
                            if (methodId.returnType != voidClassId) {
                                +tryBlock {
                                    val cachedModels = variableConstructor.valueByModelId.toMutableMap()
                                    try {
                                        variableConstructor.valueByModelId = mutableMapOf()
                                        val returnStatements =
                                            variableConstructor.getOrCreateVariable(
                                                models.first(),
                                                "mock${mockID}var$modelIndex"
                                            )
                                        returnStatement { returnStatements }
                                    } finally {
                                        variableConstructor.valueByModelId = cachedModels
                                    }
                                }.catch(Throwable::class.id) {
                                    throwStatement {
                                        val constructor =
                                            IllegalStateException::class.java.id.allConstructors.first { it.parameters.isEmpty() }
                                        constructor()
                                    }
                                }
                            }
                        }
                    },
                    exceptions = methodId.exceptions.toSet(),
                    annotations = emptyList()
                )
            }
        val methodsCluster = CgSimpleRegion(null, methods)
        val classDeclaration = buildClass {
            id = mockClassId
            interfaces.add(model.classId)
            body = buildClassBody(mockClassId) {
                methodRegions += CgMethodsCluster(header = null, content = listOf(methodsCluster))
            }
        }
        currentBlock += CgClassAsStatement(classDeclaration)
        ++mockID
        val constructorId = ConstructorId(mockClassId, emptyList())
        return newVar(model.classId, baseName) {
            CgConstructorCall(constructorId, emptyList())
        }
    }

    override fun mock(clazz: CgExpression): CgMethodCall {
        TODO("Not yet implemented")
    }

    override fun `when`(call: CgExecutableCall): CgMethodCall {
        TODO("Not yet implemented")
    }

    private class CgClassAsStatement(
        val id: ClassId,
        val documentation: CgDocumentationComment?,
        val annotations: List<CgAnnotation>,
        val superclass: ClassId?,
        val interfaces: List<ClassId>,
        val body: CgClassBody,
        val isStatic: Boolean,
        val isNested: Boolean
    ) : CgStatement {
        constructor(cgClass: CgClass) : this(
            cgClass.id,
            cgClass.documentation,
            cgClass.annotations,
            cgClass.superclass,
            cgClass.interfaces,
            cgClass.body,
            cgClass.isStatic,
            cgClass.isNested
        )

        val packageName
            get() = id.packageName

        val simpleName
            get() = id.simpleName
    }

}

class MockFrameworkManager(context: CgContext) : CgVariableConstructorComponent(context) {

    private val objectMocker = MockitoMocker(context)
    private val staticMocker = when (context.staticsMocking) {
        is NoStaticMocking -> null
        is MockitoStaticMocking -> MockitoStaticMocker(context, objectMocker)
        else -> null
    }

    /**
     * Precondition: in the given [model] flag [UtCompositeModel.isMock] must be true.
     * @return a variable representing a created mock object.
     */
    fun createMockFor(model: UtCompositeModel, baseName: String): CgVariable = withMockFramework {
        require(model.isMock) { "Mock model is expected in MockObjectConstructor" }

        objectMocker.createMock(model, baseName)
    }

    fun mockNewInstance(mock: UtNewInstanceInstrumentation) {
        staticMocker?.mockNewInstance(mock)
    }

    fun mockStaticMethodsOfClass(classId: ClassId, methodMocks: List<UtStaticMethodInstrumentation>) {
        staticMocker?.mockStaticMethodsOfClass(classId, methodMocks)
    }

    override fun clearExecutionResources() {
        staticMocker?.clearExecutionResources()
    }

    internal fun getAndClearMethodResources(): List<CgDeclaration>? =
        if (staticMocker is MockitoStaticMocker) staticMocker.copyAndClearMockResources() else null
}

private abstract class ObjectMocker(
        context: CgContext
) : CgVariableConstructorComponent(context) {
    abstract fun createMock(model: UtCompositeModel, baseName: String): CgVariable

    abstract fun mock(clazz: CgExpression): CgMethodCall

    abstract fun `when`(call: CgExecutableCall): CgMethodCall
}

private abstract class StaticMocker(
        context: CgContext
) : CgVariableConstructorComponent(context) {
    abstract fun mockNewInstance(mock: UtNewInstanceInstrumentation)
    abstract fun mockStaticMethodsOfClass(classId: ClassId, methodMocks: List<UtStaticMethodInstrumentation>)
}

private class MockitoMocker(context: CgContext) : ObjectMocker(context) {
    override fun createMock(model: UtCompositeModel, baseName: String): CgVariable {
        // create mock object
        val modelClass = getClassOf(model.classId)
        val mockObject = newVar(model.classId, baseName = baseName, isMock = true) { mock(modelClass) }

        for ((executable, values) in model.mocks) {
            // void method
            if (executable.returnType == voidClassId) {
                // void methods on mocks do nothing by default
                continue
            }

            when (executable) {
                is MethodId -> {
                    if (executable.parameters.any { !it.isAccessibleFrom(testClassPackageName) }) {
                        error("Cannot mock method $executable with not accessible parameters" )
                    }

                    val matchers = mockitoArgumentMatchersFor(executable)
                    if (!executable.isAccessibleFrom(testClassPackageName)) {
                        error("Cannot mock method $executable as it is not accessible from package $testClassPackageName")
                    }

                    val results = values.map { variableConstructor.getOrCreateVariable(it) }.toTypedArray()
                    `when`(mockObject[executable](*matchers)).thenReturn(executable.returnType, *results)
                }
                else -> error("ConstructorId was not expected to appear in simple mocker but got $executable")
            }
        }

        return mockObject
    }

    override fun mock(clazz: CgExpression): CgMethodCall =
            mockitoClassId[mockMethodId](clazz)

    override fun `when`(call: CgExecutableCall): CgMethodCall =
            mockitoClassId[whenMethodId](call)
}

private class MockitoStaticMocker(context: CgContext, private val mocker: ObjectMocker) : StaticMocker(context) {
    private val resources = mutableListOf<CgDeclaration>()
    private val mockedStaticForMethods = mutableMapOf<ClassId, CgDeclaration>()
    private val mockedStaticConstructions = mutableSetOf<ClassId>()

    override fun mockNewInstance(mock: UtNewInstanceInstrumentation) {
        val classId = mock.classId
        if (classId in mockedStaticConstructions) return

        val mockClassCounter = CgDeclaration(
            atomicIntegerClassId,
            variableConstructor.constructVarName(MOCK_CLASS_COUNTER_NAME),
            CgConstructorCall(ConstructorId(atomicIntegerClassId, emptyList()), emptyList())
        )
        +mockClassCounter

        val mocksExecutablesAnswers = mock
            .instances
            .filterIsInstance<UtCompositeModel>()
            .filter { it.isMock }
            .map { it.mocks }

        val modelClass = getClassOf(classId)

        val mockConstructionInitializer = mockConstruction(
            modelClass,
            classId,
            mocksExecutablesAnswers,
            mockClassCounter.variable
        )
        val mockedConstructionDeclaration = CgDeclaration(
            MockitoStaticMocking.mockedConstructionClassId,
            variableConstructor.constructVarName(MOCKED_CONSTRUCTION_NAME),
            mockConstructionInitializer
        )
        resources += mockedConstructionDeclaration
        +CgAssignment(mockedConstructionDeclaration.variable, mockConstructionInitializer)
        mockedStaticConstructions += classId
    }

    override fun mockStaticMethodsOfClass(classId: ClassId, methodMocks: List<UtStaticMethodInstrumentation>) {
        for ((methodId, values) in methodMocks) {
            if (methodId.parameters.any { !it.isAccessibleFrom(testClassPackageName) }) {
                error("Cannot mock static method $methodId with not accessible parameters" )
            }

            val matchers = mockitoArgumentMatchersFor(methodId)
            val mockedStaticDeclaration = getOrCreateMockStatic(classId)
            val mockedStaticVariable = mockedStaticDeclaration.variable
            val methodRunnable = if (matchers.isEmpty()) {
                CgStaticRunnable(type = methodId.returnType, classId, methodId)
            } else {
                CgAnonymousFunction(
                    type = methodId.returnType,
                    parameters = emptyList(),
                    listOf(
                        CgStatementExecutableCall(
                            CgMethodCall(
                        caller = null,
                        methodId,
                        matchers.toList()
                    )
                        )
                    )
                )
            }
            // void method
            if (methodId.returnType == voidClassId) {
                // we do not generate additional code for void methods because they do nothing by default
                continue
            }

            if (!methodId.isAccessibleFrom(testClassPackageName)) {
                error("Cannot mock static method $methodId as it is not accessible from package $testClassPackageName")
            }

            val results = values.map { variableConstructor.getOrCreateVariable(it) }.toTypedArray()
            `when`(mockedStaticVariable, methodRunnable).thenReturn(methodId.returnType, *results)
        }
    }

    override fun clearExecutionResources() {
        resources.clear()
        mockedStaticForMethods.clear()
        mockedStaticConstructions.clear()
    }

    private fun getOrCreateMockStatic(classId: ClassId): CgDeclaration =
        mockedStaticForMethods.getOrPut(classId) {
            val modelClass = getClassOf(classId)
            val classMockStaticCall = mockStatic(modelClass)
            val mockedStaticVariableName = variableConstructor.constructVarName(MOCKED_STATIC_NAME)
            CgDeclaration(
                MockitoStaticMocking.mockedStaticClassId,
                mockedStaticVariableName,
                classMockStaticCall
            ).also {
                resources += it
                +CgAssignment(it.variable, classMockStaticCall)
            }
        }

    private fun mockConstruction(
        clazz: CgExpression,
        classId: ClassId,
        mocksWhenAnswers: List<MutableMap<ExecutableId, List<UtModel>>>,
        mockClassCounter: CgVariable
    ): CgMethodCall {
        val mockParameter = variableConstructor.declareParameter(
            classId,
            variableConstructor.constructVarName(classId.simpleName, isMock = true)
        )
        val contextParameter = variableConstructor.declareParameter(
            mockedConstructionContextClassId,
            variableConstructor.constructVarName("context")
        )

        val caseLabels = mutableListOf<CgSwitchCaseLabel>()
        for ((index, mockWhenAnswers) in mocksWhenAnswers.withIndex()) {
            val statements = mutableListOf<CgStatement>()
            for ((executable, values) in mockWhenAnswers) {
                if (executable.returnType == voidClassId) continue

                when (executable) {
                    is MethodId -> {
                        val matchers = mockitoArgumentMatchersFor(executable)
                        val results = values.map { variableConstructor.getOrCreateVariable(it) }.toTypedArray()
                        statements += CgStatementExecutableCall(
                            mocker.`when`(mockParameter[executable](*matchers))[thenReturnMethodId](*results)
                        )
                    }
                    else -> error("Expected MethodId but got ConstructorId $executable")
                }
            }

            caseLabels += CgSwitchCaseLabel(CgLiteral(intClassId, index), statements)
        }

        val switchCase = CgSwitchCase(mockClassCounter[atomicIntegerGet](), caseLabels)

        val answersBlock = CgAnonymousFunction(
            voidClassId,
            listOf(mockParameter, contextParameter).map { CgParameterDeclaration(it, isVararg = false) },
            listOf(switchCase, CgStatementExecutableCall(mockClassCounter[atomicIntegerGetAndIncrement]()))
        )

        return mockitoClassId[MockitoStaticMocking.mockConstructionMethodId](clazz, answersBlock)
    }

    private fun mockStatic(clazz: CgExpression): CgMethodCall =
        mockitoClassId[MockitoStaticMocking.mockStaticMethodId](clazz)

    private fun `when`(
        mockedStatic: CgVariable,
        runnable: CgExpression,
    ): CgMethodCall {
        val typeParams = when (runnable) {
            is CgRunnable, is CgAnonymousFunction -> listOf(runnable.type)
            else -> error("Unsupported runnable type: $runnable")
        }
        return CgMethodCall(
            mockedStatic,
            MockitoStaticMocking.mockedStaticWhen(nullable = mockedStatic.type.isNullable),
            listOf(runnable),
            TypeParameters(typeParams),
        )
    }


    fun copyAndClearMockResources(): List<CgDeclaration>? {
        val copiedResources = resources.toList()
        clearExecutionResources()

        return copiedResources.ifEmpty { null }
    }

    companion object {
        private const val MOCKED_CONSTRUCTION_NAME = "mockedConstruction"
        private const val MOCKED_STATIC_NAME = "mockedStatic"
        private const val MOCK_CLASS_COUNTER_NAME = "mockClassCounter"
    }
}
