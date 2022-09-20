package org.utbot.framework.codegen.model.constructor.context

import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.HangingTestsTimeout
import org.utbot.framework.codegen.Import
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.model.constructor.tree.Block
import org.utbot.framework.codegen.model.constructor.util.EnvironmentFieldStateCache
import org.utbot.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.framework.codegen.model.tree.CgAnnotation
import org.utbot.framework.codegen.model.tree.CgExecutableCall
import org.utbot.framework.codegen.model.tree.CgStatement
import org.utbot.framework.codegen.model.tree.CgStatementExecutableCall
import org.utbot.framework.codegen.model.tree.CgTestMethod
import org.utbot.framework.codegen.model.tree.CgThisInstance
import org.utbot.framework.codegen.model.tree.CgValue
import org.utbot.framework.codegen.model.tree.CgVariable
import java.util.IdentityHashMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.codegen.model.constructor.builtin.TestClassUtilMethodProvider
import org.utbot.framework.codegen.model.constructor.builtin.UtilClassFileMethodProvider
import org.utbot.framework.codegen.model.constructor.builtin.UtilMethodProvider
import org.utbot.framework.codegen.model.constructor.TestClassContext
import org.utbot.framework.codegen.model.constructor.TestClassModel
import org.utbot.framework.codegen.model.tree.CgParameterKind
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isCheckedException
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.framework.plugin.api.util.jClass

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
    val outerMostTestClass: ClassId

    // test class currently being generated (if series of nested classes is generated, it is the innermost one)
    var currentTestClass: ClassId

    // provider of util methods used for the test class currently being generated
    val utilMethodProvider: UtilMethodProvider

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

    val importedStaticMethods: MutableSet<MethodId>
    val importedClasses: MutableSet<ClassId>

    // util methods required by the test class being built
    val requiredUtilMethods: MutableSet<MethodId>

    val utilMethodsUsed: Boolean

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

    val parametrizedTestSource: ParametrizedTestSource

    /**
     * Flag indicating whether a mock framework is used in the generated code
     * NOTE! This flag is not about whether a mock framework is present
     * in the user's project dependencies or not.
     * This flag is true if the generated test class contains at least one mock object,
     * and false otherwise. See method [withMockFramework].
     */
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

    // Variables of java.lang.reflect.Field type declared in the current name scope
    var declaredFieldRefs: PersistentMap<FieldId, CgVariable>

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

    var allExecutions: List<UtExecution>

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

    fun addExceptionIfNeeded(exception: ClassId) {
        if (exception !is BuiltinClassId) {
            require(exception isSubtypeOf Throwable::class.id) {
                "Class $exception which is not a Throwable was passed"
            }

            val isUnchecked = !exception.jClass.isCheckedException
            val alreadyAdded =
                collectedExceptions.any { existingException -> exception isSubtypeOf existingException }

            if (isUnchecked || alreadyAdded) return

            collectedExceptions
                .removeIf { existingException -> existingException isSubtypeOf exception }
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
        val prevDeclaredFieldRefs = declaredFieldRefs
        val prevValueByModel = IdentityHashMap(valueByModel)
        val prevValueByModelId = valueByModelId.toMutableMap()
        return try {
            block()
        } finally {
            existingVariableNames = prevVariableNames
            declaredClassRefs = prevDeclaredClassRefs
            declaredExecutableRefs = prevDeclaredExecutableRefs
            declaredFieldRefs = prevDeclaredFieldRefs
            valueByModel = prevValueByModel
            valueByModelId = prevValueByModelId
        }
    }

    /**
     * [ClassId] of a class that contains util methods.
     * For example, it can be the current test class, or it can be a generated separate `UtUtils` class.
     */
    val utilsClassId: ClassId
        get() = utilMethodProvider.utilClassId

    /**
     * Checks is it our util reflection field getter method.
     * When this method is used with type cast in Kotlin, this type cast have to be safety
     */
    val MethodId.isGetFieldUtilMethod: Boolean
        get() = this == utilMethodProvider.getFieldValueMethodId
                || this == utilMethodProvider.getStaticFieldValueMethodId

    val testClassThisInstance: CgThisInstance

    // util methods and auxiliary classes of current test class

    val capturedArgumentClass: ClassId
        get() = utilMethodProvider.capturedArgumentClassId

    val getUnsafeInstance: MethodId
        get() = utilMethodProvider.getUnsafeInstanceMethodId

    val createInstance: MethodId
        get() = utilMethodProvider.createInstanceMethodId

    val createArray: MethodId
        get() = utilMethodProvider.createArrayMethodId

    val setField: MethodId
        get() = utilMethodProvider.setFieldMethodId

    val setStaticField: MethodId
        get() = utilMethodProvider.setStaticFieldMethodId

    val getFieldValue: MethodId
        get() = utilMethodProvider.getFieldValueMethodId

    val getStaticFieldValue: MethodId
        get() = utilMethodProvider.getStaticFieldValueMethodId

    val getEnumConstantByName: MethodId
        get() = utilMethodProvider.getEnumConstantByNameMethodId

    val deepEquals: MethodId
        get() = utilMethodProvider.deepEqualsMethodId

    val arraysDeepEquals: MethodId
        get() = utilMethodProvider.arraysDeepEqualsMethodId

    val iterablesDeepEquals: MethodId
        get() = utilMethodProvider.iterablesDeepEqualsMethodId

    val streamsDeepEquals: MethodId
        get() = utilMethodProvider.streamsDeepEqualsMethodId

    val mapsDeepEquals: MethodId
        get() = utilMethodProvider.mapsDeepEqualsMethodId

    val hasCustomEquals: MethodId
        get() = utilMethodProvider.hasCustomEqualsMethodId

    val getArrayLength: MethodId
        get() = utilMethodProvider.getArrayLengthMethodId

    val buildStaticLambda: MethodId
        get() = utilMethodProvider.buildStaticLambdaMethodId

    val buildLambda: MethodId
        get() = utilMethodProvider.buildLambdaMethodId

    val getLookupIn: MethodId
        get() = utilMethodProvider.getLookupInMethodId

    val getSingleAbstractMethod: MethodId
        get() = utilMethodProvider.getSingleAbstractMethodMethodId

    val getLambdaCapturedArgumentTypes: MethodId
        get() = utilMethodProvider.getLambdaCapturedArgumentTypesMethodId

    val getLambdaCapturedArgumentValues: MethodId
        get() = utilMethodProvider.getLambdaCapturedArgumentValuesMethodId

    val getInstantiatedMethodType: MethodId
        get() = utilMethodProvider.getInstantiatedMethodTypeMethodId

    val getLambdaMethod: MethodId
        get() = utilMethodProvider.getLambdaMethodMethodId
}

/**
 * Context with current code generation info
 */
internal data class CgContext(
    override val classUnderTest: ClassId,
    val generateUtilClassFile: Boolean,
    override var currentExecutable: ExecutableId? = null,
    override val collectedExceptions: MutableSet<ClassId> = mutableSetOf(),
    override val collectedMethodAnnotations: MutableSet<CgAnnotation> = mutableSetOf(),
    override val collectedImports: MutableSet<Import> = mutableSetOf(),
    override val importedStaticMethods: MutableSet<MethodId> = mutableSetOf(),
    override val importedClasses: MutableSet<ClassId> = mutableSetOf(),
    override val requiredUtilMethods: MutableSet<MethodId> = mutableSetOf(),
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
    override val parametrizedTestSource: ParametrizedTestSource = ParametrizedTestSource.DO_NOT_PARAMETRIZE,
    override var mockFrameworkUsed: Boolean = false,
    override var currentBlock: PersistentList<CgStatement> = persistentListOf(),
    override var existingVariableNames: PersistentSet<String> = persistentSetOf(),
    override var declaredClassRefs: PersistentMap<ClassId, CgVariable> = persistentMapOf(),
    override var declaredExecutableRefs: PersistentMap<ExecutableId, CgVariable> = persistentMapOf(),
    override var declaredFieldRefs: PersistentMap<FieldId, CgVariable> = persistentMapOf(),
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
    override lateinit var allExecutions: List<UtExecution>

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

    override val outerMostTestClass: ClassId by lazy {
        val packagePrefix = if (testClassPackageName.isNotEmpty()) "$testClassPackageName." else ""
        val simpleName = testClassCustomName ?: "${classUnderTest.simpleName}Test"
        val name = "$packagePrefix$simpleName"
        BuiltinClassId(
            name = name,
            canonicalName = name,
            simpleName = simpleName
        )
    }

    /**
     * Determine where the util methods will come from.
     * If we don't want to use a separately generated util class,
     * util methods will be generated directly in the test class (see [TestClassUtilMethodProvider]).
     * Otherwise, an util class will be generated separately and we will use util methods from it (see [UtilClassFileMethodProvider]).
     */
    override val utilMethodProvider: UtilMethodProvider
        get() = if (generateUtilClassFile) {
            UtilClassFileMethodProvider
        } else {
            TestClassUtilMethodProvider(outerMostTestClass)
        }

    override lateinit var currentTestClass: ClassId

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

    private fun createClassIdForNestedClass(testClassModel: TestClassModel): ClassId {
        val simpleName = "${testClassModel.classUnderTest.simpleName}Test"
        return BuiltinClassId(
            name = currentTestClass.name + "$" + simpleName,
            canonicalName = currentTestClass.canonicalName + "." + simpleName,
            simpleName = simpleName
        )
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

    override val utilMethodsUsed: Boolean
        get() = requiredUtilMethods.isNotEmpty()
}