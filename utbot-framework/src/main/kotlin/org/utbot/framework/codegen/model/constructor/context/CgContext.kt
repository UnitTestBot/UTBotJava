package org.utbot.framework.codegen.model.constructor.context

import kotlinx.collections.immutable.*
import kotlinx.coroutines.runBlocking
import org.utbot.framework.codegen.*
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
 * For example, [currentTestClass] and [currentExecutable] can be reassigned
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

    // test class currently being generated
    val currentTestClass: BuiltinClassId

    // current executable under test
    var currentExecutable: ExecutableId?

    // test class superclass (if needed)
    var testClassSuperclass: ClassId?

    // list of interfaces that the test class must inherit
    val collectedTestClassInterfaces: MutableSet<ClassId>

    // list of annotations of the test class
    val collectedTestClassAnnotations: MutableSet<CgAnnotation>

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

    fun <R> withClassScope(block: () -> R): R {
        clearClassScope()
        return try {
            block()
        } finally {
            clearClassScope()
        }
    }

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

    private fun clearClassScope() {
        collectedImports.clear()
        importedStaticMethods.clear()
        importedClasses.clear()
        testMethods.clear()
        requiredUtilMethods.clear()
        valueByModel.clear()
        valueByModelId.clear()
        mockFrameworkUsed = false
    }

    /**
     * Check whether a method is an util method of the current class
     */
    val MethodExecutableId.isUtil: Boolean
        get() = methodId in currentTestClass.possibleUtilMethodIds

    /**
     * Checks is it our util reflection field getter method.
     * When this method is used with type cast in Kotlin, this type cast have to be safety
     */
    val MethodExecutableId.isGetFieldUtilMethod: Boolean
        get() = isUtil && (name == getFieldValue.name || name == getStaticFieldValue.name)

    val testClassThisInstance: CgThisInstance

    // util methods of current test class

    val getUnsafeInstance: MethodId
        get() = currentTestClass.getUnsafeInstanceMethodId

    val createInstance: MethodId
        get() = currentTestClass.createInstanceMethodId

    val createArray: MethodId
        get() = currentTestClass.createArrayMethodId

    val setField: MethodId
        get() = currentTestClass.setFieldMethodId

    val setStaticField: MethodId
        get() = currentTestClass.setStaticFieldMethodId

    val getFieldValue: MethodId
        get() = currentTestClass.getFieldValueMethodId

    val getStaticFieldValue: MethodId
        get() = currentTestClass.getStaticFieldValueMethodId

    val getEnumConstantByName: MethodId
        get() = currentTestClass.getEnumConstantByNameMethodId

    val deepEquals: MethodId
        get() = currentTestClass.deepEqualsMethodId

    val arraysDeepEquals: MethodId
        get() = currentTestClass.arraysDeepEqualsMethodId

    val iterablesDeepEquals: MethodId
        get() = currentTestClass.iterablesDeepEqualsMethodId

    val streamsDeepEquals: MethodId
        get() = currentTestClass.streamsDeepEqualsMethodId

    val mapsDeepEquals: MethodId
        get() = currentTestClass.mapsDeepEqualsMethodId

    val hasCustomEquals: MethodId
        get() = currentTestClass.hasCustomEqualsMethodId

    val getArrayLength: MethodId
        get() = currentTestClass.getArrayLengthMethodId
}

/**
 * Context with current code generation info
 */
internal data class CgContext(
    override val classUnderTest: ClassId,
    override var currentExecutable: ExecutableId? = null,
    override val collectedTestClassInterfaces: MutableSet<ClassId> = mutableSetOf(),
    override val collectedTestClassAnnotations: MutableSet<CgAnnotation> = mutableSetOf(),
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
    override val enableTestsTimeout: Boolean = true
) : CgContextOwner {
    override lateinit var statesCache: EnvironmentFieldStateCache
    override lateinit var actual: CgVariable

    override val currentTestClass: BuiltinClassId get() {
        val packagePrefix = if (testClassPackageName.isNotEmpty()) "$testClassPackageName." else ""
        val simpleName = testClassCustomName ?: "${createTestClassName(classUnderTest.name)}Test"
        val name = "$packagePrefix$simpleName"
        return builtInClass(name)
    }

    override var testClassSuperclass: ClassId? = null
        set(value) {
            // Assigning a value to the testClassSuperclass when it is already non-null
            // means that we need the test class to have more than one superclass
            // which is impossible in Java and Kotlin.
            require(field == null) { "It is impossible for the test class to have more than one superclass" }
            field = value
        }

    override var valueByModel: IdentityHashMap<UtModel, CgValue> = IdentityHashMap()

    override var valueByModelId: MutableMap<Int?, CgValue> = mutableMapOf()

    override val currentMethodParameters: MutableMap<CgParameterKind, CgVariable> = mutableMapOf()

    override val testClassThisInstance: CgThisInstance = CgThisInstance(currentTestClass)
}