package org.utbot.framework.codegen.model.constructor.tree

import kotlinx.collections.immutable.PersistentList
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.TestNg
import org.utbot.framework.codegen.model.constructor.builtin.any
import org.utbot.framework.codegen.model.constructor.builtin.anyOfClass
import org.utbot.framework.codegen.model.constructor.builtin.getMethodId
import org.utbot.framework.codegen.model.constructor.builtin.getTargetException
import org.utbot.framework.codegen.model.constructor.builtin.invoke
import org.utbot.framework.codegen.model.constructor.builtin.newInstance
import org.utbot.framework.codegen.model.constructor.builtin.setAccessible
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.tree.CgCallableAccessManagerImpl.FieldAccessorSuitability.*
import org.utbot.framework.codegen.model.constructor.util.CgComponents
import org.utbot.framework.codegen.model.constructor.util.getAmbiguousOverloadsOf
import org.utbot.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.framework.codegen.model.constructor.util.isUtil
import org.utbot.framework.codegen.model.constructor.util.typeCast
import org.utbot.framework.codegen.model.tree.CgAllocateArray
import org.utbot.framework.codegen.model.tree.CgAssignment
import org.utbot.framework.codegen.model.tree.CgConstructorCall
import org.utbot.framework.codegen.model.tree.CgExecutableCall
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgFieldAccess
import org.utbot.framework.codegen.model.tree.CgMethodCall
import org.utbot.framework.codegen.model.tree.CgSpread
import org.utbot.framework.codegen.model.tree.CgStatement
import org.utbot.framework.codegen.model.tree.CgStaticFieldAccess
import org.utbot.framework.codegen.model.tree.CgThisInstance
import org.utbot.framework.codegen.model.tree.CgValue
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.util.at
import org.utbot.framework.codegen.model.util.isAccessibleFrom
import org.utbot.framework.codegen.model.util.canBeReadFrom
import org.utbot.framework.codegen.model.util.nullLiteral
import org.utbot.framework.codegen.model.util.resolve
import org.utbot.framework.plugin.api.BuiltinConstructorId
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.exceptions
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.method
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId

typealias Block = PersistentList<CgStatement>

class CgIncompleteMethodCall(val method: MethodId, val caller: CgExpression?)

/**
 * Provides DSL methods for method and field access elements creation
 *
 * Checks the accessibility of methods and fields and replaces
 * direct access with reflective access when needed
 */
interface CgCallableAccessManager {
    operator fun CgExpression?.get(methodId: MethodId): CgIncompleteMethodCall

    operator fun ClassId.get(staticMethodId: MethodId): CgIncompleteMethodCall

    operator fun ConstructorId.invoke(vararg args: Any?): CgExecutableCall

    operator fun CgIncompleteMethodCall.invoke(vararg args: Any?): CgMethodCall

    // non-static fields
    operator fun CgExpression.get(fieldId: FieldId): CgExpression

    // static fields
    operator fun ClassId.get(fieldId: FieldId): CgStaticFieldAccess
}

internal class CgCallableAccessManagerImpl(val context: CgContext) : CgCallableAccessManager,
    CgContextOwner by context {

    private val statementConstructor by lazy { CgComponents.getStatementConstructorBy(context) }

    private val variableConstructor by lazy { CgComponents.getVariableConstructorBy(context) }

    override operator fun CgExpression?.get(methodId: MethodId): CgIncompleteMethodCall =
        CgIncompleteMethodCall(methodId, this)

    override operator fun ClassId.get(staticMethodId: MethodId): CgIncompleteMethodCall =
        CgIncompleteMethodCall(staticMethodId, null)

    override operator fun ConstructorId.invoke(vararg args: Any?): CgExecutableCall {
        val resolvedArgs = args.resolve()
        val constructorCall = if (this canBeCalledWith resolvedArgs) {
            CgConstructorCall(this, resolvedArgs.guardedForDirectCallOf(this))
        } else {
            callWithReflection(resolvedArgs)
        }
        newConstructorCall(this)
        return constructorCall
    }

    override operator fun CgIncompleteMethodCall.invoke(vararg args: Any?): CgMethodCall {
        val resolvedArgs = args.resolve()
        val methodCall = if (method.canBeCalledWith(caller, resolvedArgs)) {
            CgMethodCall(caller, method, resolvedArgs.guardedForDirectCallOf(method))
        } else {
            method.callWithReflection(caller, resolvedArgs)
        }
        newMethodCall(method)
        return methodCall
    }

    override operator fun CgExpression.get(fieldId: FieldId): CgExpression {
        return when (val suitability = fieldId.accessSuitability(this)) {
            // receiver (a.k.a. caller) is suitable, so we access field directly
            is Suitable -> CgFieldAccess(this, fieldId)
            // receiver has to be type cast, and then we can access the field
            is RequiresTypeCast -> {
                CgFieldAccess(
                    caller = typeCast(suitability.targetType, this),
                    fieldId
                )
            }
            // we can access the field only via reflection
            is ReflectionOnly -> fieldId.accessWithReflection(this)
        }
    }

    override operator fun ClassId.get(fieldId: FieldId): CgStaticFieldAccess = CgStaticFieldAccess(fieldId)

    private fun newMethodCall(methodId: MethodId) {
        if (isUtil(methodId)) requiredUtilMethods += methodId
        importIfNeeded(methodId)

        //Builtin methods does not have jClass, so [methodId.method] will crash on it,
        //so we need to collect required exceptions manually from source codes
        if (methodId is BuiltinMethodId) {
            methodId.findExceptionTypes()
                .forEach { addExceptionIfNeeded(it) }
            return
        }

        if (methodId == getTargetException) {
            addExceptionIfNeeded(Throwable::class.id)
        }

        val methodIsUnderTestAndThrowsExplicitly = methodId == currentExecutable
                && currentExecution?.result is UtExplicitlyThrownException
        val frameworkSupportsAssertThrows = testFramework == Junit5 || testFramework == TestNg

        //If explicit exception is wrapped with assertThrows,
        // no "throws" in test method signature is required.
        if (methodIsUnderTestAndThrowsExplicitly && frameworkSupportsAssertThrows) {
            return
        }

        methodId.method.exceptionTypes.forEach { addExceptionIfNeeded(it.id) }
    }

    private fun newConstructorCall(constructorId: ConstructorId) {
        importIfNeeded(constructorId.classId)

        // Builtin constructors do not have jClass, so [constructorId.exceptions] will crash on it,
        // so we need to collect required exceptions manually from source codes (see BuiltinConstructorId.findExceptionTypes()).

        if (constructorId is BuiltinConstructorId) {
            constructorId.findExceptionTypes().forEach { addExceptionIfNeeded(it) }
            return
        }

        for (exception in constructorId.exceptions) {
            addExceptionIfNeeded(exception)
        }
    }

    private infix fun CgExpression.canBeReceiverOf(executable: MethodId): Boolean =
        when {
            // method of the current test class can be called on its 'this' instance
            currentTestClass == executable.classId && this isThisInstanceOf currentTestClass -> true
            // method of a class can be called on an object of this class or any of its subtypes
            this.type isSubtypeOf executable.classId -> true
            else -> false
        }

    private infix fun CgExpression.canBeArgOf(type: ClassId): Boolean {
        // TODO: SAT-1210 support generics so that we wouldn't need to check specific cases such as this one
        if (this is CgExecutableCall && (executableId == any || executableId == anyOfClass)) {
            return true
        }
        return this == nullLiteral() && type.isAccessibleFrom(testClassPackageName)
                || this.type isSubtypeOf type
    }

    private infix fun CgExpression?.isThisInstanceOf(classId: ClassId): Boolean =
        this is CgThisInstance && this.type == classId

    /**
     * Check whether @receiver (list of expressions) is a valid list of arguments for [executableId]
     *
     * First, we check all arguments except for the last one.
     * It is done to consider the last argument separately since it can be a vararg,
     * which requires some additional checks.
     *
     * For the last argument there can be several cases:
     * - Last argument is not of array type - then we simply check this argument as all the others
     * - Last argument is of array type:
     *     - Given arguments and parameters have the same size
     *         - Last argument is an array and it matches last parameter array type
     *         - Last argument is a single element of a vararg parameter - then we check
     *           if argument's type matches the vararg element's type
     *     - Given arguments and parameters have different size (last parameter is vararg) - then we
     *       check if all of the given arguments match the vararg element's type
     *
     */
    private infix fun List<CgExpression>.canBeArgsOf(executableId: ExecutableId): Boolean {
        val paramTypes = executableId.parameters

        // no arguments case
        if (paramTypes.isEmpty()) {
            return this.isEmpty()
        }

        val paramTypesExceptLast = paramTypes.dropLast(1)
        val lastParamType = paramTypes.last()

        // considering all arguments except the last one
        for ((arg, paramType) in (this zip paramTypesExceptLast)) {
            if (!(arg canBeArgOf paramType)) return false
        }

        // when the last parameter is not of array type
        if (!lastParamType.isArray) {
            val lastArg = this.last()
            return lastArg canBeArgOf lastParamType
        }

        // when arguments and parameters have equal size
        if (size == paramTypes.size) {
            val lastArg = this.last()
            return when {
                // last argument matches last param type
                lastArg canBeArgOf lastParamType -> true
                // last argument is a single element of a vararg parameter
                lastArg canBeArgOf lastParamType.elementClassId!! -> true
                else -> false
            }
        }

        // when arguments size is greater than the parameters size
        // meaning that the last parameter is vararg
        return subList(paramTypes.size - 1, size).all {
            it canBeArgOf lastParamType.elementClassId!!
        }
    }

    /**
     * @return true if a method can be called with the given arguments without reflection
     */
    private fun MethodId.canBeCalledWith(caller: CgExpression?, args: List<CgExpression>): Boolean {
        // Check method accessibility.
        if (!isAccessibleFrom(testClassPackageName)) {
            return false
        }

        // Check arguments suitability.
        if (!(args canBeArgsOf this)) {
            return false
        }

        // If method is static, then it may not have a caller.
        if (this.isStatic && caller == null) {
            return true
        }

        if (this.isStatic && caller != null && codegenLanguage == CodegenLanguage.KOTLIN) {
            error("In Kotlin, unlike Java, static methods cannot be called on an instance: $this")
        }

        // If method is from current test class, then it may not have a caller.
        if (this.classId == currentTestClass && caller == null) {
            return true
        }

        requireNotNull(caller) { "Method must have a caller, unless it is the method of the current test class or a static method: $this" }
        return caller canBeReceiverOf this
    }

    private fun FieldId.accessSuitability(accessor: CgExpression?): FieldAccessorSuitability {
        // Check field accessibility.
        if (!canBeReadFrom(context)) {
            return ReflectionOnly
        }

        // If field is static, then it may not have an accessor.
        if (this.isStatic && accessor == null) {
            return Suitable
        }

        if (this.isStatic && accessor != null && codegenLanguage == CodegenLanguage.KOTLIN) {
            error("In Kotlin, unlike Java, static fields cannot be accessed by an object: $this")
        }

        // if field is declared in the current test class
        if (this.declaringClass == currentTestClass) {
            return when {
                // field of the current class can be accessed without explicit accessor
                accessor == null -> Suitable
                // field of the current class can be accessed with `this` reference
                accessor isThisInstanceOf currentTestClass -> Suitable
                // field of the current class can be accessed by the instance of this class
                accessor.type isSubtypeOf currentTestClass -> Suitable
                // in any other case, we have to use reflection
                else -> ReflectionOnly
            }
        }

        requireNotNull(accessor) {
            "Field access must have a non-null accessor, unless it is the field of the current test class or a static field: $this"
        }

        if (this.declaringClass == accessor.type) {
            // if the field was declared in class `T`, and the accessor is __exactly__ of this type (not a subtype),
            // then we can safely access the field
            return Suitable
        }

        val fieldDeclaringClassType = this.declaringClass
        val accessorType = accessor.type

        if (fieldDeclaringClassType is BuiltinClassId || accessorType is BuiltinClassId) {
            return Suitable
        }

        // The rest of the logic of this method processes hidden fields.
        // We cannot check the fields of builtin classes, so we assume that we work correctly with them,
        // because they are well known library classes (e.g. Mockito) that are unlikely to have hidden fields.

        return when {
            accessorType isSubtypeOf fieldDeclaringClassType -> {
                if (this isFieldHiddenIn accessorType.jClass) {
                    // We know that declaring class of field is accessible,
                    // because it was checked in `isAccessibleFrom` call at the start of this method,
                    // so we can safely do a type cast.
                    RequiresTypeCast(fieldDeclaringClassType)
                } else {
                    Suitable
                }
            }
            fieldDeclaringClassType isSubtypeOf accessorType -> {
                // We know that declaring class of field is accessible,
                // because it was checked in `isAccessibleFrom` call at the start of this method,
                // so we can safely do a type cast.
                RequiresTypeCast(fieldDeclaringClassType)
            }
            // Accessor type is not subtype or supertype of the field's declaring class.
            // So the only remaining option to access the field is to use reflection.
            else -> ReflectionOnly
        }
    }

    /**
     * Check if the field represented by @receiver is hidden when accessed from an instance of [subclass].
     *
     * Brief description: this method collects all types from hierarchy in between [subclass] (inclusive)
     * up to the class where the field is declared (exclusive) and checks if any of these classes declare
     * a field with the same name (thus hiding the given field). For examples and more details read documentation below.
     *
     * The **contract** of this method is as follows:
     * [subclass] must be a subclass of `this.declaringClass` (and they **must not** be the same class).
     * That is because if they are equal (we try to access field from instance of its own class),
     * then the field is not hidden. And if [subclass] is actually not a subclass, but a superclass of a class
     * where the field is declared, then this check makes no sense (superclass cannot hide field of a subclass).
     * Lastly, if these classes are not related in terms of inheritance, then there must be some error,
     * because such checks also make no sense.
     *
     * **Examples**.
     *
     * For example, given classes:
     * ```
     * class A { int x; }
     * class B extends A { int x; }
     * ```
     * we can say that field `x` of class `A` is hidden when we are dealing with instances of a subclass `B`:
     * ```
     * B b = new B();
     * b.x = 10;
     * ```
     * There is no way to access field `x` of class `A` from variable `b` without using type casts or reflection.
     *
     * So the result of [isFieldHiddenIn] for field `x` of class `A` and a subclass `B` would be `true`.
     *
     * **NOTE** that there can be more complicated cases. For example, interfaces can also have fields (they are always static).
     * Fields of an interface will be available to the classes and interfaces that inherit from it.
     * That means that when checking if a field is hidden we have to take **both** classes and interfaces into account.
     *
     * **However**, there is an important detail. We **do not** consider superclasses and superinterfaces
     * of the type where the field (represented by @receiver) was declared. Consider the following example:
     * ```
     * class A { int x; }
     * class B extends A { int x; }
     * class C extends B { }
     * ```
     * If we are checking if the field `x` of class `B` is hidden when accessed by an instance of class `C`,
     * then [isFieldHiddenIn] will return `false`, because the field `x` of class `A` does not stand in the way
     * and is itself hidden by `B.x`. That is why we **can** access `B.x` from a variable of type `C`.
     *
     * Lastly, another **important** example:
     * ```
     * class A { int x; }
     * interface I1 { int x = 10; }
     * class B extends A implements I1 { }
     * ```
     * Unlike previous examples, here we have class `B` which has different "branches" of supertypes.
     * On the one hand we have superclass `A` and on the other we have interface `I1`.
     * `A` and `I1` do not have any relationship with each other, but `B` **can** access fields `x` from both of them.
     * However, it **must** be done with a type cast (to `A` or `I1`), because otherwise accessing field `x` will
     * be ambiguous. Method [isFieldHiddenIn] **does consider** such cases as well.
     *
     * **These examples show** that when checking if a field is hidden we **must** consider all supertypes
     * in all "branches" **up to** the type where the field is declared (**exclusively**).
     * Then we check if any of these types has a field with the same name.
     * If such field is found, then the field is hidden, otherwise - not.
     */
    private infix fun FieldId.isFieldHiddenIn(subclass: Class<*>): Boolean {
        // see the documentation of this method for more details on this requirement
        require(subclass.id != this.declaringClass) {
            "A given subclass must not be equal to the declaring class of the field: $subclass"
        }

        // supertypes (classes and interfaces) from subclass (inclusive) up to superclass (exclusive)
        val supertypes = sequence {
            var types = generateSequence(subclass) { it.superclass }
                .takeWhile { it != declaringClass.jClass }
                .toList()
            while (types.isNotEmpty()) {
                yieldAll(types)
                types = types.flatMap { it.interfaces.toList() }
            }
        }

        // check if any of the collected supertypes declare a field with the same name
        val fieldHidingTypes = supertypes.toList()
            .filter { type ->
                val fieldNames = type.declaredFields.map { it.name }
                this.name in fieldNames
            }

        // if we found at least one type that hides the field, then the field is hidden
        return fieldHidingTypes.isNotEmpty()
    }

    /**
     * @return true if a constructor can be called with the given arguments without reflection
     */
    private infix fun ConstructorId.canBeCalledWith(args: List<CgExpression>): Boolean =
        isAccessibleFrom(testClassPackageName) && !classId.isAbstract && args canBeArgsOf this

    private fun List<CgExpression>.guardedForDirectCallOf(executable: ExecutableId): List<CgExpression> {
        val ambiguousOverloads = executable.classId
            .getAmbiguousOverloadsOf(executable)
            .filterNot { it == executable }
            .toList()

        val isEmptyAmbiguousOverloads = ambiguousOverloads.isEmpty()

        return if (isEmptyAmbiguousOverloads) this else castAmbiguousArguments(executable, this, ambiguousOverloads)
    }

    private fun castAmbiguousArguments(
        executable: ExecutableId,
        args: List<CgExpression>,
        ambiguousOverloads: List<ExecutableId>
    ): List<CgExpression> =
        args.withIndex().map { (i ,arg) ->
            val targetType = executable.parameters[i]

            // always cast nulls
            if (arg == nullLiteral()) return@map typeCast(targetType, arg)

            // in case arg type exactly equals target type, do nothing
            if (arg.type == targetType) return@map arg

            // arg type is subtype of target type
            // check other overloads for ambiguous types
            val typesInOverloadings = ambiguousOverloads.map { it.parameters[i] }
            val ancestors = typesInOverloadings.filter { arg.type.isSubtypeOf(it) }

            if (ancestors.isNotEmpty()) typeCast(targetType, arg) else arg
        }

    /**
     * Receives a list of [CgExpression].
     * Transforms it into a list of [CgExpression] where:
     * - array and literal values are cast to [java.lang.Object]
     * - other values remain as they were
     *
     * @return a list of [CgExpression] where each expression can be
     * used as an argument of reflective call to a method or constructor
     */
    private fun List<CgExpression>.guardedForReflectiveCall(): List<CgExpression> =
        map {
            when {
                it is CgValue && it.type.isArray -> typeCast(objectClassId, it)
                it == nullLiteral() -> typeCast(objectClassId, it)
                else -> it
            }
        }

    private fun FieldId.accessWithReflection(accessor: CgExpression?): CgMethodCall {
        val field = declaredFieldRefs[this]
            ?: statementConstructor.createFieldVariable(this).also {
                declaredFieldRefs = declaredFieldRefs.put(this, it)
                +it[setAccessible](true)
            }
        return field[getMethodId](accessor)
    }

    private fun MethodId.callWithReflection(caller: CgExpression?, args: List<CgExpression>): CgMethodCall {
        containsReflectiveCall = true
        val method = getExecutableRefVariable(args)
        val arguments = args.guardedForReflectiveCall().toTypedArray()
        val argumentsArrayVariable = convertVarargToArray(method, arguments)

        return method[invoke](caller, CgSpread(argumentsArrayVariable.type, argumentsArrayVariable))
    }

    private fun ConstructorId.callWithReflection(args: List<CgExpression>): CgExecutableCall {
        containsReflectiveCall = true
        val constructor = getExecutableRefVariable(args)
        val arguments = args.guardedForReflectiveCall().toTypedArray()
        val argumentsArrayVariable = convertVarargToArray(constructor, arguments)

        return constructor[newInstance](argumentsArrayVariable)
    }

    private fun ExecutableId.getExecutableRefVariable(arguments: List<CgExpression>): CgVariable {
        return declaredExecutableRefs[this]
            ?: statementConstructor.createExecutableVariable(this, arguments).also {
                declaredExecutableRefs = declaredExecutableRefs.put(this, it)
                +it[setAccessible](true)
            }
    }

    private fun convertVarargToArray(reflectionCallVariable: CgVariable, arguments: Array<CgExpression>): CgVariable {
        val argumentsArrayVariable = variableConstructor.newVar(
            baseType = objectArrayClassId,
            baseName = "${reflectionCallVariable.name}Arguments"
        ) {
            CgAllocateArray(
                type = objectArrayClassId,
                elementType = objectClassId,
                size = arguments.size
            )
        }

        for ((i, argument) in arguments.withIndex()) {
            +CgAssignment(argumentsArrayVariable.at(i), argument)
        }

        return argumentsArrayVariable
    }

    private fun BuiltinConstructorId.findExceptionTypes(): Set<ClassId> {
        // At the moment we do not have builtin ids for constructors that throw exceptions,
        // so we have this trivial when-expression. But if we ever add ids for such constructors,
        // then we **must** specify their exceptions here, so that we take them into account when generating code.
        @Suppress("UNUSED_EXPRESSION")
        return when (this) {
            else -> emptySet()
        }
    }

    //WARN: if you make changes in the following sets of exceptions,
    //don't forget to change them in hardcoded [UtilMethods] as well
    private fun BuiltinMethodId.findExceptionTypes(): Set<ClassId> {
        // TODO: at the moment we treat BuiltinMethodIds that are not util method ids
        // as if they have no exceptions. This should be fixed by storing exception types in BuiltinMethodId
        // or allowing us to access actual java.lang.Class for classes from mockito and other libraries
        // (this could be possibly solved by using user project's class loaders in UtContext)
        if (!isUtil(this)) return emptySet()

        with(utilMethodProvider) {
            return when (this@findExceptionTypes) {
                getEnumConstantByNameMethodId -> setOf(IllegalAccessException::class.id)
                getStaticFieldValueMethodId,
                setStaticFieldMethodId -> setOf(java.lang.IllegalAccessException::class.id, java.lang.NoSuchFieldException::class.id)
                getFieldValueMethodId,
                setFieldMethodId -> setOf(java.lang.ClassNotFoundException::class.id, java.lang.IllegalAccessException::class.id, java.lang.NoSuchFieldException::class.id)
                createInstanceMethodId -> setOf(Exception::class.id)
                getUnsafeInstanceMethodId -> setOf(ClassNotFoundException::class.id, NoSuchFieldException::class.id, IllegalAccessException::class.id)
                createArrayMethodId -> setOf(ClassNotFoundException::class.id)
                deepEqualsMethodId,
                arraysDeepEqualsMethodId,
                iterablesDeepEqualsMethodId,
                streamsDeepEqualsMethodId,
                mapsDeepEqualsMethodId,
                hasCustomEqualsMethodId,
                getArrayLengthMethodId,
                getLambdaCapturedArgumentTypesMethodId,
                getLambdaCapturedArgumentValuesMethodId,
                getInstantiatedMethodTypeMethodId,
                getLambdaMethodMethodId,
                getSingleAbstractMethodMethodId -> emptySet()
                buildStaticLambdaMethodId,
                buildLambdaMethodId -> setOf(Throwable::class.id)
                getLookupInMethodId ->setOf(IllegalAccessException::class.id, NoSuchFieldException::class.id)
                else -> error("Unknown util method ${this@findExceptionTypes}")
            }
        }
    }

    /**
     * This sealed class describes different extents of suitability (or matching)
     * between an expression (in the role of a field accessor) and a field.
     *
     * In other words, this class and its inheritors describe if a given object (accessor)
     * can be used to access a field, and if so, then are there any additional actions required (like type cast).
     */
    private sealed class FieldAccessorSuitability {
        /**
         * Field can be accessed by a given accessor directly
         */
        object Suitable : FieldAccessorSuitability()

        /**
         * Field can be accessed by a given accessor, but it has to be type cast to the [targetType]
         */
        class RequiresTypeCast(val targetType: ClassId) : FieldAccessorSuitability()

        /**
         * Field can only be accessed by a given accessor via reflection.
         * For example, if the accessor's type is inaccessible from the current package,
         * so we cannot declare a variable of this type or perform a type cast.
         * But there may be other cases. For example, we also cannot use
         * anonymous classes' names in the code, so reflection may be required.
         */
        object ReflectionOnly : FieldAccessorSuitability()
    }
}