package org.utbot.framework.codegen.model.constructor.tree

import kotlinx.collections.immutable.PersistentList
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.TestNg
import org.utbot.framework.codegen.model.constructor.builtin.any
import org.utbot.framework.codegen.model.constructor.builtin.anyOfClass
import org.utbot.framework.codegen.model.constructor.builtin.forName
import org.utbot.framework.codegen.model.constructor.builtin.getDeclaredConstructor
import org.utbot.framework.codegen.model.constructor.builtin.getDeclaredMethod
import org.utbot.framework.codegen.model.constructor.builtin.getTargetException
import org.utbot.framework.codegen.model.constructor.builtin.invoke
import org.utbot.framework.codegen.model.constructor.builtin.newInstance
import org.utbot.framework.codegen.model.constructor.builtin.setAccessible
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.util.CgComponents
import org.utbot.framework.codegen.model.constructor.util.classCgClassId
import org.utbot.framework.codegen.model.constructor.util.getAmbiguousOverloadsOf
import org.utbot.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.framework.codegen.model.constructor.util.isUtil
import org.utbot.framework.codegen.model.constructor.util.typeCast
import org.utbot.framework.codegen.model.tree.CgAllocateArray
import org.utbot.framework.codegen.model.tree.CgAssignment
import org.utbot.framework.codegen.model.tree.CgConstructorCall
import org.utbot.framework.codegen.model.tree.CgExecutableCall
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgGetJavaClass
import org.utbot.framework.codegen.model.tree.CgMethodCall
import org.utbot.framework.codegen.model.tree.CgSpread
import org.utbot.framework.codegen.model.tree.CgStatement
import org.utbot.framework.codegen.model.tree.CgThisInstance
import org.utbot.framework.codegen.model.tree.CgValue
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.util.at
import org.utbot.framework.codegen.model.util.isAccessibleFrom
import org.utbot.framework.codegen.model.util.nullLiteral
import org.utbot.framework.codegen.model.util.resolve
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.util.exceptions
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.isSubtypeOf
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

    private fun newMethodCall(methodId: MethodId) {
        if (isUtil(methodId)) requiredUtilMethods += methodId
        importIfNeeded(methodId)

        //Builtin methods does not have jClass, so [methodId.method] will crash on it,
        //so we need to collect required exceptions manually from source codes
        if (isUtil(methodId)) {
            utilMethodProvider
                .findExceptionTypesOf(methodId)
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
        for (exception in constructorId.exceptions) {
            addExceptionIfNeeded(exception)
        }
    }

    private infix fun CgExpression?.canBeReceiverOf(executable: MethodId): Boolean =
        when {
            // TODO: rewrite by using CgMethodId, etc.
            outerMostTestClass == executable.classId && this isThisInstanceOf outerMostTestClass -> true
            executable.isStatic -> true
            else -> this?.type?.isSubtypeOf(executable.classId) ?: false
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
    private fun MethodId.canBeCalledWith(caller: CgExpression?, args: List<CgExpression>): Boolean =
        (isUtil(this) || isAccessibleFrom(testClassPackageName))
                && caller canBeReceiverOf this
                && args canBeArgsOf this

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

    private fun ExecutableId.toExecutableVariable(args: List<CgExpression>): CgVariable {
        val declaringClass = statementConstructor.newVar(Class::class.id) { classId[forName](classId.name) }
        val argTypes = (args zip parameters).map { (arg, paramType) ->
            val baseName = when (arg) {
                is CgVariable -> "${arg.name}Type"
                else -> "${paramType.prettifiedName.decapitalize()}Type"
            }
            statementConstructor.newVar(classCgClassId, baseName) {
                if (paramType.isPrimitive) {
                    CgGetJavaClass(paramType)
                } else {
                    Class::class.id[forName](paramType.name)
                }
            }
        }

        return when (this) {
            is MethodId -> {
                val name = this.name + "Method"
                statementConstructor.newVar(java.lang.reflect.Method::class.id, name) {
                    declaringClass[getDeclaredMethod](this.name, *argTypes.toTypedArray())
                }
            }
            is ConstructorId -> {
                val name = this.classId.prettifiedName.decapitalize() + "Constructor"
                statementConstructor.newVar(java.lang.reflect.Constructor::class.id, name) {
                    declaringClass[getDeclaredConstructor](*argTypes.toTypedArray())
                }
            }
        }
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

    private fun MethodId.callWithReflection(caller: CgExpression?, args: List<CgExpression>): CgMethodCall {
        containsReflectiveCall = true
        val method = declaredExecutableRefs[this]
            ?: toExecutableVariable(args).also {
                declaredExecutableRefs = declaredExecutableRefs.put(this, it)
                +it[setAccessible](true)
            }

        val arguments = args.guardedForReflectiveCall().toTypedArray()
        val argumentsArrayVariable = convertVarargToArray(method, arguments)

        return method[invoke](caller, CgSpread(argumentsArrayVariable.type, argumentsArrayVariable))
    }

    private fun ConstructorId.callWithReflection(args: List<CgExpression>): CgExecutableCall {
        containsReflectiveCall = true
        val constructor = declaredExecutableRefs[this]
            ?: this.toExecutableVariable(args).also {
                declaredExecutableRefs = declaredExecutableRefs.put(this, it)
                +it[setAccessible](true)
            }

        val arguments = args.guardedForReflectiveCall().toTypedArray()
        val argumentsArrayVariable = convertVarargToArray(constructor, arguments)

        return constructor[newInstance](argumentsArrayVariable)
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
}