package org.utbot.framework.codegen.model.constructor.context

import kotlinx.collections.immutable.*
import kotlinx.coroutines.runBlocking
import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.codegen.model.constructor.TestClassContext
import org.utbot.framework.codegen.model.constructor.TestClassModel
import org.utbot.framework.codegen.model.constructor.builtin.*
import org.utbot.framework.codegen.model.constructor.tree.Block
import org.utbot.framework.codegen.model.constructor.util.EnvironmentFieldStateCache
import org.utbot.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.codegen.model.util.createTestClassName
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isCheckedException
import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.FieldId
import org.utbot.jcdb.api.MethodId
import org.utbot.jcdb.api.isSubtypeOf
import java.util.*
import kotlin.collections.set

/**
 * Interface for all code generation context aware entities
 *
 * Most of the properties are 'val'.
 * Although, some of the properties are declared as 'var' so that
 * they can be reassigned as well as modified
 *
 * For example, [outerMostTestClass] and [currentExecutable] can be reassigned
 * when we start generating another method or test class
 *
 * [existingVariableNames] is a 'var' property
 * that can be reverted to its previous value on exit from a name scope
 *
 * @see [CgContextOwner.withNameScope]
 */
internal interface CgContextOwner {
    // current class under test
    val classUnderTest: ClassId

    // test class currently being generated (if series of nested classes is generated, it is the outermost one)
    val outerMostTestClass: BuiltinClassId

    // test class currently being generated (if series of nested classes is generated, it is the innermost one)
    var currentTestClass: BuiltinClassId

    // current executable under test
    var currentExecutable: ExecutableId?

    // ClassInfo for the outermost class currently being generated
    val outerMostTestClassContext: TestClassContext

    // If generating series of nested classes, it is ClassInfo for the innermost one,
    // otherwise it should be equal to outerMostTestClassInfo
    val currentTestClassContext: TestClassContext

    // exceptions that can be thrown inside of current method being built
    val collectedExceptions: MutableSet<ClassId>

    // annotations required by the current method being built
    val collectedMethodAnnotations: MutableSet<CgAnnotation>

    // imports required by the test class being built
    val collectedImports: MutableSet<Import>

    val importedStaticMethods: MutableSet<MethodExecutableId>
    val importedClasses: MutableSet<ClassId>

    // util methods required by the test class being built
    val requiredUtilMethods: MutableSet<MethodExecutableId>

    // test methods being generated
    val testMethods: MutableList<CgTestMethod>

    // names of methods that already exist in the test class
    val existingMethodNames: MutableSet<String>

    // At the start of a test method we save the initial state of some static fields in variables.
    // This map is used to restore the initial values of these variables at the end of the test method.
    val prevStaticFieldValues: MutableMap<FieldId, CgVariable>

    // names of parameters of methods under test
    val paramNames: Map<ExecutableId, List<String>>

    // UtExecution we currently generate a test method for.
    // It is null when no test method is being generated at the moment.
    var currentExecution: UtExecution?

    val testFramework: TestFramework

    val mockFramework: MockFramework

    val staticsMocking: StaticsMocking

    val forceStaticMocking: ForceStaticMocking

    val generateWarningsForStaticMocking: Boolean

    val codegenLanguage: CodegenLanguage

    val parameterizedTestSource: ParametrizedTestSource

    // flag indicating whether a mock framework is used in the generated code
    var mockFrameworkUsed: Boolean

    // object that represents a set of information about JUnit of selected version

    // Persistent collections are used to conveniently restore their previous state.
    // For example, when we exit a block of code we return to the previous name scope.
    // At that moment we revert some collections (e.g. variable names) to the previous state.

    // current block of code being built
    var currentBlock: PersistentList<CgStatement>

    // variable names being used in the current name scope
    var existingVariableNames: PersistentSet<String>

    // variables of java.lang.Class type declared in the current name scope
    var declaredClassRefs: PersistentMap<ClassId, CgVariable>

    // Variables of either java.lang.reflect.Constructor or java.lang.reflect.Method types
    // declared in the current name scope.
    // java.lang.reflect.Executable is a superclass of both of these types.
    var declaredExecutableRefs: PersistentMap<ExecutableId, CgVariable>

    // generated this instance for method under test
    var thisInstance: CgValue?

    // generated arguments for method under test
    val methodArguments: MutableList<CgValue>

    // a variable representing an actual result of the method under test call
    var actual: CgVariable

    // a variable representing if test method contains reflective call or not
    // and should we catch exceptions like InvocationTargetException or not so on
    var containsReflectiveCall: Boolean

    // map from a set of tests for a method to another map
    // which connects code generation error message
    // with the number of times it occurred
    val codeGenerationErrors: MutableMap<CgMethodTestSet, MutableMap<String, Int>>

    // package for generated test class
    val testClassPackageName: String

    val shouldOptimizeImports: Boolean

    var valueByModel: IdentityHashMap<UtModel, CgValue>

    // use it to compare stateBefore and result variables - in case of equality do not create new variable
    var valueByModelId: MutableMap<Int?, CgValue>

    // parameters of the method currently being generated
    val currentMethodParameters: MutableMap<CgParameterKind, CgVariable>

    val testClassCustomName: String?

    /**
     * Determines whether tests that throw Runtime exceptions should fail or pass.
     */
    val runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour

    /**
     * Timeout for possibly hanging tests (uses info from concrete executor).
     */
    val hangingTestsTimeout: HangingTestsTimeout

    /**
     * Determines whether tests with timeout should fail (with added Timeout annotation) or be disabled (for our pipeline).
     */
    val enableTestsTimeout: Boolean

    var statesCache: EnvironmentFieldStateCache

    fun block(init: () -> Unit): Block {
        val prevBlock = currentBlock
        return try {
            val block = persistentListOf<CgStatement>()
            currentBlock = block
            withNameScope {
                init()
                currentBlock
            }
        } finally {
            currentBlock = prevBlock
        }
    }

    operator fun CgStatement.unaryPlus() {
        currentBlock = currentBlock.add(this)
    }

    operator fun CgExecutableCall.unaryPlus(): CgStatementExecutableCall =
        CgStatementExecutableCall(this).also {
            currentBlock = currentBlock.add(it)
        }

    fun updateCurrentExecutable(executableId: ExecutableId) {
        currentExecutable = executableId
    }

        fun addExceptionIfNeeded(exception: ClassId) = runBlocking {
        if (exception !is BuiltinClassId) {
            require(exception isSubtypeOf Throwable::class.id) {
                "Class $exception which is not a Throwable was passed"
            }

            val isUnchecked = !exception.isCheckedException
            val alreadyAdded =
                collectedExceptions.any { existingException -> exception isSubtypeOf existingException }

            if (isUnchecked || alreadyAdded) return@runBlocking

            collectedExceptions
                .removeIf { existingException -> existingException blockingIsSubtypeOf exception }
        }

        if (collectedExceptions.add(exception)) {
            importIfNeeded(exception)
        }
    }

    fun addAnnotation(annotation: CgAnnotation) {
        if (collectedMethodAnnotations.add(annotation)) {
            importIfNeeded(annotation.classId) // TODO: check how JUnit annotations are loaded
        }
    }

    /**
     * This method sets up context for a new test class file generation and executes the given [block].
     * Afterwards, context is set back to the initial state.
     */
    fun <R> withTestClassFileScope(block: () -> R): R

    /**
     * This method sets up context for a new test class generation and executes the given [block].
     * Afterwards, context is set back to the initial state.
     */
    fun <R> withTestClassScope(block: () -> R): R

    /**
     * This method does almost all the same as [withTestClassScope], but for nested test classes.
     * The difference is that instead of working with [outerMostTestClassContext] it works with [currentTestClassContext].
     */
    fun <R> withNestedClassScope(testClassModel: TestClassModel, block: () -> R): R

    /**
     * Set [mockFrameworkUsed] flag to true if the block is successfully executed
     */
    fun <R> withMockFramework(block: () -> R): R {
        val result = block()
        mockFrameworkUsed = true
        return result
    }

    fun updateVariableScope(variable: CgVariable, model: UtModel? = null) {
        model?.let {
            valueByModel[it] = variable
            (model as UtReferenceModel).let { refModel ->
                refModel.id.let { id -> valueByModelId[id] = variable }
            }
        }
    }

    fun <R> withNameScope(block: () -> R): R {
        val prevVariableNames = existingVariableNames
        val prevDeclaredClassRefs = declaredClassRefs
        val prevDeclaredExecutableRefs = declaredExecutableRefs
        val prevValueByModel = IdentityHashMap(valueByModel)
        val prevValueByModelId = valueByModelId.toMutableMap()
        return try {
            block()
        } finally {
            existingVariableNames = prevVariableNames
            declaredClassRefs = prevDeclaredClassRefs
            declaredExecutableRefs = prevDeclaredExecutableRefs
            valueByModel = prevValueByModel
            valueByModelId = prevValueByModelId
        }
    }

    /**
     * Check whether a method is an util method of the current class
     */
    val MethodExecutableId.isUtil: Boolean
        get() = methodId in outerMostTestClass.possibleUtilMethodIds

    /**
     * Checks is it our util reflection field getter method.
     * When this method is used with type cast in Kotlin, this type cast have to be safety
     */
    val MethodExecutableId.isGetFieldUtilMethod: Boolean
        get() = isUtil && (name == getFieldValue.name || name == getStaticFieldValue.name)

    val testClassThisInstance: CgThisInstance

    // util methods of current test class

    val getUnsafeInstance: MethodId
        get() = outerMostTestClass.getUnsafeInstanceMethodId

    val createInstance: MethodId
        get() = outerMostTestClass.createInstanceMethodId

    val createArray: MethodId
        get() = outerMostTestClass.createArrayMethodId

    val setField: MethodId
        get() = outerMostTestClass.setFieldMethodId

    val setStaticField: MethodId
        get() = outerMostTestClass.setStaticFieldMethodId

    val getFieldValue: MethodId
        get() = outerMostTestClass.getFieldValueMethodId

    val getStaticFieldValue: MethodId
        get() = outerMostTestClass.getStaticFieldValueMethodId

    val getEnumConstantByName: MethodId
        get() = outerMostTestClass.getEnumConstantByNameMethodId

    val deepEquals: MethodId
        get() = outerMostTestClass.deepEqualsMethodId

    val arraysDeepEquals: MethodId
        get() = outerMostTestClass.arraysDeepEqualsMethodId

    val iterablesDeepEquals: MethodId
        get() = outerMostTestClass.iterablesDeepEqualsMethodId

    val streamsDeepEquals: MethodId
        get() = outerMostTestClass.streamsDeepEqualsMethodId

    val mapsDeepEquals: MethodId
        get() = outerMostTestClass.mapsDeepEqualsMethodId

    val hasCustomEquals: MethodId
        get() = outerMostTestClass.hasCustomEqualsMethodId

    val getArrayLength: MethodId
        get() = outerMostTestClass.getArrayLengthMethodId
}

/**
 * Context with current code generation info
 */
internal data class CgContext(
    override val classUnderTest: ClassId,
    override var currentExecutable: ExecutableId? = null,
    override val collectedExceptions: MutableSet<ClassId> = mutableSetOf(),
    override val collectedMethodAnnotations: MutableSet<CgAnnotation> = mutableSetOf(),
    override val collectedImports: MutableSet<Import> = mutableSetOf(),
    override val importedStaticMethods: MutableSet<MethodExecutableId> = mutableSetOf(),
    override val importedClasses: MutableSet<ClassId> = mutableSetOf(),
    override val requiredUtilMethods: MutableSet<MethodExecutableId> = mutableSetOf(),
    override val testMethods: MutableList<CgTestMethod> = mutableListOf(),
    override val existingMethodNames: MutableSet<String> = mutableSetOf(),
    override val prevStaticFieldValues: MutableMap<FieldId, CgVariable> = mutableMapOf(),
    override val paramNames: Map<ExecutableId, List<String>>,
    override var currentExecution: UtExecution? = null,
    override val testFramework: TestFramework,
    override val mockFramework: MockFramework,
    override val staticsMocking: StaticsMocking,
    override val forceStaticMocking: ForceStaticMocking,
    override val generateWarningsForStaticMocking: Boolean,
    override val codegenLanguage: CodegenLanguage = CodegenLanguage.defaultItem,
    override val parameterizedTestSource: ParametrizedTestSource = ParametrizedTestSource.DO_NOT_PARAMETRIZE,
    override var mockFrameworkUsed: Boolean = false,
    override var currentBlock: PersistentList<CgStatement> = persistentListOf(),
    override var existingVariableNames: PersistentSet<String> = persistentSetOf(),
    override var declaredClassRefs: PersistentMap<ClassId, CgVariable> = persistentMapOf(),
    override var declaredExecutableRefs: PersistentMap<ExecutableId, CgVariable> = persistentMapOf(),
    override var thisInstance: CgValue? = null,
    override val methodArguments: MutableList<CgValue> = mutableListOf(),
    override val codeGenerationErrors: MutableMap<CgMethodTestSet, MutableMap<String, Int>> = mutableMapOf(),
    override val testClassPackageName: String = classUnderTest.packageName,
    override var shouldOptimizeImports: Boolean = false,
    override var testClassCustomName: String? = null,
    override val runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour =
        RuntimeExceptionTestsBehaviour.defaultItem,
    override val hangingTestsTimeout: HangingTestsTimeout = HangingTestsTimeout(),
    override val enableTestsTimeout: Boolean = true,
    override var containsReflectiveCall: Boolean = false,
) : CgContextOwner {
    override lateinit var statesCache: EnvironmentFieldStateCache
    override lateinit var actual: CgVariable

    /**
     * This property cannot be accessed outside of test class file scope
     * (i.e. outside of [CgContextOwner.withTestClassFileScope]).
     */
    override val outerMostTestClassContext: TestClassContext
        get() = _outerMostTestClassContext ?: error("Accessing outerMostTestClassInfo out of class file scope")

    private var _outerMostTestClassContext: TestClassContext? = null

    /**
     * This property cannot be accessed outside of test class scope
     * (i.e. outside of [CgContextOwner.withTestClassScope]).
     */
    override val currentTestClassContext: TestClassContext
        get() = _currentTestClassContext ?: error("Accessing currentTestClassInfo out of class scope")

    private var _currentTestClassContext: TestClassContext? = null

    override val outerMostTestClass: BuiltinClassId get() {
        val packagePrefix = if (testClassPackageName.isNotEmpty()) "$testClassPackageName." else ""
        val simpleName = testClassCustomName ?: "${createTestClassName(classUnderTest.name)}Test"
        val name = "$packagePrefix$simpleName"
        return builtInClass(name)
    }

    override lateinit var currentTestClass: BuiltinClassId

    override fun <R> withTestClassFileScope(block: () -> R): R {
        clearClassScope()
        _outerMostTestClassContext = TestClassContext()
        return try {
            block()
        } finally {
            clearClassScope()
        }
    }

    override fun <R> withTestClassScope(block: () -> R): R {
        _currentTestClassContext = outerMostTestClassContext
        currentTestClass = outerMostTestClass
        return try {
            block()
        } finally {
            _currentTestClassContext = null
        }
    }

    override fun <R> withNestedClassScope(testClassModel: TestClassModel, block: () -> R): R {
        val previousCurrentTestClassInfo = currentTestClassContext
        val previousCurrentTestClass = currentTestClass
        currentTestClass = createClassIdForNestedClass(testClassModel)
        _currentTestClassContext = TestClassContext()
        return try {
            block()
        } finally {
            _currentTestClassContext = previousCurrentTestClassInfo
            currentTestClass = previousCurrentTestClass
        }
    }

    private fun createClassIdForNestedClass(testClassModel: TestClassModel): BuiltinClassId {
        val simpleName = "${testClassModel.classUnderTest.simpleName}Test"
        return builtInClass(currentTestClass.name + "$" + simpleName, isNested = true)
    }

    private fun clearClassScope() {
        _outerMostTestClassContext = null
        collectedImports.clear()
        importedStaticMethods.clear()
        importedClasses.clear()
        testMethods.clear()
        requiredUtilMethods.clear()
        valueByModel.clear()
        valueByModelId.clear()
        mockFrameworkUsed = false
    }


    override var valueByModel: IdentityHashMap<UtModel, CgValue> = IdentityHashMap()

    override var valueByModelId: MutableMap<Int?, CgValue> = mutableMapOf()

    override val currentMethodParameters: MutableMap<CgParameterKind, CgVariable> = mutableMapOf()

    override val testClassThisInstance: CgThisInstance = CgThisInstance(outerMostTestClass)
}