package org.utbot.framework.codegen.model.constructor.util

import org.utbot.framework.codegen.model.constructor.builtin.forName
import org.utbot.framework.codegen.model.constructor.builtin.mockMethodId
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.tree.CgCallableAccessManager
import org.utbot.framework.codegen.model.tree.CgAllocateArray
import org.utbot.framework.codegen.model.tree.CgAnnotation
import org.utbot.framework.codegen.model.tree.CgAnonymousFunction
import org.utbot.framework.codegen.model.tree.CgComment
import org.utbot.framework.codegen.model.tree.CgDeclaration
import org.utbot.framework.codegen.model.tree.CgEmptyLine
import org.utbot.framework.codegen.model.tree.CgEnumConstantAccess
import org.utbot.framework.codegen.model.tree.CgErrorWrapper
import org.utbot.framework.codegen.model.tree.CgExecutableCall
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgForEachLoopBuilder
import org.utbot.framework.codegen.model.tree.CgForLoopBuilder
import org.utbot.framework.codegen.model.tree.CgGetClass
import org.utbot.framework.codegen.model.tree.CgIfStatement
import org.utbot.framework.codegen.model.tree.CgInnerBlock
import org.utbot.framework.codegen.model.tree.CgLiteral
import org.utbot.framework.codegen.model.tree.CgLogicalAnd
import org.utbot.framework.codegen.model.tree.CgLogicalOr
import org.utbot.framework.codegen.model.tree.CgMethodCall
import org.utbot.framework.codegen.model.tree.CgMultilineComment
import org.utbot.framework.codegen.model.tree.CgMultipleArgsAnnotation
import org.utbot.framework.codegen.model.tree.CgNamedAnnotationArgument
import org.utbot.framework.codegen.model.tree.CgParameterDeclaration
import org.utbot.framework.codegen.model.tree.CgReturnStatement
import org.utbot.framework.codegen.model.tree.CgSingleArgAnnotation
import org.utbot.framework.codegen.model.tree.CgSingleLineComment
import org.utbot.framework.codegen.model.tree.CgThrowStatement
import org.utbot.framework.codegen.model.tree.CgTryCatch
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.tree.buildAssignment
import org.utbot.framework.codegen.model.tree.buildCgForEachLoop
import org.utbot.framework.codegen.model.tree.buildDeclaration
import org.utbot.framework.codegen.model.tree.buildDoWhileLoop
import org.utbot.framework.codegen.model.tree.buildForLoop
import org.utbot.framework.codegen.model.tree.buildTryCatch
import org.utbot.framework.codegen.model.tree.buildWhileLoop
import org.utbot.framework.codegen.model.util.buildExceptionHandler
import org.utbot.framework.codegen.model.util.isAccessibleFrom
import org.utbot.framework.codegen.model.util.nullLiteral
import org.utbot.framework.codegen.model.util.resolve
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.executable
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isNotSubtypeOf
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId
import fj.data.Either
import org.utbot.framework.codegen.model.constructor.builtin.getDeclaredConstructor
import org.utbot.framework.codegen.model.constructor.builtin.getDeclaredField
import org.utbot.framework.codegen.model.constructor.builtin.getDeclaredMethod
import org.utbot.framework.codegen.model.tree.CgArrayInitializer
import org.utbot.framework.codegen.model.tree.CgGetJavaClass
import org.utbot.framework.codegen.model.tree.CgIsInstance
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.classClassId
import org.utbot.framework.plugin.api.util.constructorClassId
import org.utbot.framework.plugin.api.util.fieldClassId
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.methodClassId
import org.utbot.framework.plugin.api.util.denotableType
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.kotlinFunction

interface CgStatementConstructor {
    fun newVar(baseType: ClassId, baseName: String? = null, init: () -> CgExpression): CgVariable =
        newVar(baseType, model = null, baseName, isMutable = false, init)

    fun newVar(
        baseType: ClassId,
        baseName: String? = null,
        isMutable: Boolean = false,
        init: () -> CgExpression
    ): CgVariable =
        newVar(baseType, model = null, baseName, isMutable = isMutable, init)

    fun newVar(
        baseType: ClassId,
        model: UtModel? = null,
        baseName: String? = null,
        isMutable: Boolean = false,
        init: () -> CgExpression
    ): CgVariable = newVar(baseType, model, baseName, isMock = false, isMutable = isMutable, init)

    fun newVar(
        baseType: ClassId,
        model: UtModel? = null,
        baseName: String? = null,
        isMock: Boolean = false,
        isMutable: Boolean = false,
        init: () -> CgExpression
    ): CgVariable

    fun createDeclarationForNewVarAndUpdateVariableScopeOrGetExistingVariable(
        baseType: ClassId,
        model: UtModel? = null,
        baseName: String? = null,
        isMock: Boolean = false,
        isMutableVar: Boolean = false,
        init: () -> CgExpression
    ): Either<CgDeclaration, CgVariable>

    // assignment
    infix fun CgExpression.`=`(value: Any?)
    infix fun CgExpression.and(other: CgExpression): CgLogicalAnd
    infix fun CgExpression.or(other: CgExpression): CgLogicalOr
    fun ifStatement(condition: CgExpression, trueBranch: () -> Unit, falseBranch: (() -> Unit)? = null): CgIfStatement
    fun forLoop(init: CgForLoopBuilder.() -> Unit)
    fun whileLoop(condition: CgExpression, statements: () -> Unit)
    fun doWhileLoop(condition: CgExpression, statements: () -> Unit)
    fun forEachLoop(init: CgForEachLoopBuilder.() -> Unit)

    fun getClassOf(classId: ClassId): CgExpression

    /**
     * Create a variable of type [java.lang.reflect.Field] by the given [FieldId].
     */
    fun createFieldVariable(fieldId: FieldId): CgVariable


    /**
     * Given an [executableId] that represents method or constructor and a list of arguments for it,
     * create a variable of type [java.lang.reflect.Method] or [java.lang.reflect.Constructor].
     * This created variable is returned.
     */
    fun createExecutableVariable(executableId: ExecutableId, arguments: List<CgExpression>): CgVariable

    fun tryBlock(init: () -> Unit): CgTryCatch
    fun tryBlock(init: () -> Unit, resources: List<CgDeclaration>?): CgTryCatch
    fun CgTryCatch.catch(exception: ClassId, init: (CgVariable) -> Unit): CgTryCatch
    fun CgTryCatch.finally(init: () -> Unit): CgTryCatch

    fun CgExpression.isInstance(value: CgExpression): CgIsInstance

    fun innerBlock(init: () -> Unit): CgInnerBlock

//    fun CgTryCatchBuilder.statements(init: () -> Unit)
//    fun CgTryCatchBuilder.handler(exception: ClassId, init: (CgVariable) -> Unit)

    fun comment(text: String): CgComment
    fun comment(): CgComment

    fun multilineComment(lines: List<String>): CgComment

    fun lambda(type: ClassId, vararg parameters: CgVariable, body: () -> Unit): CgAnonymousFunction

    fun annotation(classId: ClassId, argument: Any?): CgAnnotation
    fun annotation(classId: ClassId, namedArguments: List<Pair<String, CgExpression>>): CgAnnotation
    fun annotation(
        classId: ClassId,
        buildArguments: MutableList<Pair<String, CgExpression>>.() -> Unit = {}
    ): CgAnnotation

    fun returnStatement(expression: () -> CgExpression)

    // Throw statement
    fun throwStatement(exception: () -> CgExpression): CgThrowStatement

    fun emptyLine()
    fun emptyLineIfNeeded()

    // utils

    fun declareParameter(type: ClassId, name: String): CgVariable = declareVariable(type, name)

    fun declareVariable(type: ClassId, name: String): CgVariable

    fun guardExpression(baseType: ClassId, expression: CgExpression): ExpressionWithType

    fun wrapTypeIfRequired(baseType: ClassId): ClassId
}

internal class CgStatementConstructorImpl(context: CgContext) :
    CgStatementConstructor,
    CgContextOwner by context,
    CgCallableAccessManager by CgComponents.getCallableAccessManagerBy(context) {

    private val nameGenerator = CgComponents.getNameGeneratorBy(context)

    override fun createDeclarationForNewVarAndUpdateVariableScopeOrGetExistingVariable(
        baseType: ClassId,
        model: UtModel?,
        baseName: String?,
        isMock: Boolean,
        isMutableVar: Boolean,
        init: () -> CgExpression
    ): Either<CgDeclaration, CgVariable> {
        // check if we can use base type and initializer directly
        // if not, fall back to using reflection
        val baseExpr = init()
        val (type, expr) = when (baseExpr) {
            is CgEnumConstantAccess -> guardEnumConstantAccess(baseExpr)
            is CgAllocateArray -> guardArrayAllocation(baseExpr)
            is CgArrayInitializer -> guardArrayInitializer(baseExpr)
            is CgExecutableCall -> guardExecutableCall(baseType, baseExpr)
            else -> guardExpression(baseType, baseExpr)
        }

        val classRef = classRefOrNull(type, expr)
        if (classRef in declaredClassRefs) {
            return Either.right(declaredClassRefs[classRef]!!)
        }

        val name = when {
            classRef != null && baseName == null -> {
                val base = classRef.prettifiedName.decapitalize()
                nameGenerator.variableName(base + "Clazz")
            }
            // we use baseType here intentionally
            else -> nameGenerator.variableName(baseType, baseName, isMock)
        }

        importIfNeeded(type)

        val declaration = buildDeclaration {
            variableType = type
            variableName = name
            initializer = expr
            isMutable = isMutableVar
        }

        classRef?.let { declaredClassRefs = declaredClassRefs.put(it, declaration.variable) }
        updateVariableScope(declaration.variable, model)

        return Either.left(declaration)
    }

    override fun newVar(
        baseType: ClassId,
        model: UtModel?,
        baseName: String?,
        isMock: Boolean,
        isMutable: Boolean,
        init: () -> CgExpression
    ): CgVariable {
        // it is important that we use a denotable type for declaration, because that allows
        // us to avoid creating `Object` variables for instances of anonymous classes,
        // where we can instead use the supertype of the anonymous class
        val declarationOrVar: Either<CgDeclaration, CgVariable> =
            createDeclarationForNewVarAndUpdateVariableScopeOrGetExistingVariable(
                baseType.denotableType,
                model,
                baseName,
                isMock,
                isMutable,
                init
            )

        return declarationOrVar.either(
            { declaration ->
                currentBlock += declaration

                declaration.variable
            },
            { variable -> variable }
        )
    }

    override fun CgExpression.`=`(value: Any?) {
        currentBlock += buildAssignment {
            lValue = this@`=`
            rValue = value.resolve()
        }
    }

    override fun CgExpression.and(other: CgExpression): CgLogicalAnd =
        CgLogicalAnd(this, other)

    override fun CgExpression.or(other: CgExpression): CgLogicalOr =
        CgLogicalOr(this, other)

    override fun ifStatement(condition: CgExpression, trueBranch: () -> Unit, falseBranch: (() -> Unit)?): CgIfStatement {
        val trueBranchBlock = block(trueBranch)
        val falseBranchBlock = falseBranch?.let { block(it) }
        return CgIfStatement(condition, trueBranchBlock, falseBranchBlock).also {
            currentBlock += it
        }
    }

    override fun forLoop(init: CgForLoopBuilder.() -> Unit) = withNameScope {
        currentBlock += buildForLoop(init)
    }

    override fun whileLoop(condition: CgExpression, statements: () -> Unit) {
        currentBlock += buildWhileLoop {
            this.condition = condition
            this.statements += block(statements)
        }
    }

    override fun doWhileLoop(condition: CgExpression, statements: () -> Unit) {
        currentBlock += buildDoWhileLoop {
            this.condition = condition
            this.statements += block(statements)
        }
    }

    override fun forEachLoop(init: CgForEachLoopBuilder.() -> Unit) = withNameScope {
        currentBlock += buildCgForEachLoop(init)
    }

    /**
     * @return expression for [java.lang.Class] of the given [classId]
     */
    override fun getClassOf(classId: ClassId): CgExpression {
        return if (classId isAccessibleFrom testClassPackageName) {
            CgGetJavaClass(classId)
        } else {
            newVar(classCgClassId) { classClassId[forName](classId.name) }
        }
    }


    override fun createFieldVariable(fieldId: FieldId): CgVariable {
        val declaringClass = newVar(classClassId) { classClassId[forName](fieldId.declaringClass.name) }
        val name = fieldId.name + "Field"
        return newVar(fieldClassId, name) {
            declaringClass[getDeclaredField](fieldId.name)
        }
    }

    override fun createExecutableVariable(executableId: ExecutableId, arguments: List<CgExpression>): CgVariable {
        val declaringClass = newVar(classClassId) { classClassId[forName](executableId.classId.name) }
        val argTypes = (arguments zip executableId.parameters).map { (argument, parameterType) ->
            val baseName = when (argument) {
                is CgVariable -> "${argument.name}Type"
                else -> "${parameterType.prettifiedName.decapitalize()}Type"
            }
            newVar(classCgClassId, baseName) {
                if (parameterType.isPrimitive) {
                    CgGetJavaClass(parameterType)
                } else {
                    classClassId[forName](parameterType.name)
                }
            }
        }

        return when (executableId) {
            is MethodId -> {
                val name = executableId.name + "Method"
                newVar(methodClassId, name) {
                    declaringClass[getDeclaredMethod](executableId.name, *argTypes.toTypedArray())
                }
            }
            is ConstructorId -> {
                val name = executableId.classId.prettifiedName.decapitalize() + "Constructor"
                newVar(constructorClassId, name) {
                    declaringClass[getDeclaredConstructor](*argTypes.toTypedArray())
                }
            }
        }
    }

    override fun tryBlock(init: () -> Unit): CgTryCatch = tryBlock(init, null)

    override fun tryBlock(init: () -> Unit, resources: List<CgDeclaration>?): CgTryCatch =
        buildTryCatch {
            statements = block(init)
            this.resources = resources
        }

    override fun CgTryCatch.catch(exception: ClassId, init: (CgVariable) -> Unit): CgTryCatch {
        val newHandler = buildExceptionHandler {
            val e = declareVariable(exception, nameGenerator.variableName(exception.simpleName.decapitalize()))
            this.exception = e
            this.statements = block { init(e) }
        }
        return this.copy(handlers = handlers + newHandler)
    }

    override fun CgTryCatch.finally(init: () -> Unit): CgTryCatch {
        val finallyBlock = block(init)
        return this.copy(finally = finallyBlock)
    }

    override fun CgExpression.isInstance(value: CgExpression): CgIsInstance {
        require(this.type == classClassId) {
            "isInstance method can be called on object with type $classClassId only, but actual type is ${this.type}"
        }

        //TODO: we should better process it as this[isInstanceMethodId](value) as it is a call
        return CgIsInstance(this, value)
    }

    override fun innerBlock(init: () -> Unit): CgInnerBlock =
        CgInnerBlock(block(init)).also {
            currentBlock += it
        }

    override fun comment(text: String): CgComment =
        CgSingleLineComment(text).also {
            currentBlock += it
        }

    override fun comment(): CgComment =
        CgSingleLineComment("").also {
            currentBlock += it
        }

    override fun multilineComment(lines: List<String>): CgComment =
        CgMultilineComment(lines).also {
            currentBlock += it
        }

    override fun lambda(type: ClassId, vararg parameters: CgVariable, body: () -> Unit): CgAnonymousFunction {
        return withNameScope {
            for (parameter in parameters) {
                declareParameter(parameter.type, parameter.name)
            }
            val paramDeclarations = parameters.map { CgParameterDeclaration(it) }
            CgAnonymousFunction(type, paramDeclarations, block(body))
        }
    }

    override fun annotation(classId: ClassId, argument: Any?): CgAnnotation {
        val annotation = CgSingleArgAnnotation(classId, argument.resolve())
        addAnnotation(annotation)
        return annotation
    }

    override fun annotation(classId: ClassId, namedArguments: List<Pair<String, CgExpression>>): CgAnnotation {
        val annotation = CgMultipleArgsAnnotation(
            classId,
            namedArguments.mapTo(mutableListOf()) { (name, value) -> CgNamedAnnotationArgument(name, value) }
        )
        addAnnotation(annotation)
        return annotation
    }

    override fun annotation(
        classId: ClassId,
        buildArguments: MutableList<Pair<String, CgExpression>>.() -> Unit
    ): CgAnnotation {
        val arguments = mutableListOf<Pair<String, CgExpression>>()
            .apply(buildArguments)
            .map { (name, value) -> CgNamedAnnotationArgument(name, value) }
        val annotation = CgMultipleArgsAnnotation(classId, arguments.toMutableList())
        addAnnotation(annotation)
        return annotation
    }

    override fun returnStatement(expression: () -> CgExpression) {
        currentBlock += CgReturnStatement(expression())
    }

    // Throw statement

    override fun throwStatement(exception: () -> CgExpression): CgThrowStatement =
        CgThrowStatement(exception()).also { currentBlock += it }

    override fun emptyLine() {
        currentBlock += CgEmptyLine()
    }

    /**
     * Add an empty line if the current block has at least one statement
     * and the current last statement is not an empty line.
     */
    override fun emptyLineIfNeeded() {
        val lastStatement = currentBlock.lastOrNull() ?: return
        if (lastStatement is CgEmptyLine) return
        emptyLine()
    }

    override fun declareVariable(type: ClassId, name: String): CgVariable =
        CgVariable(name, type).also {
            updateVariableScope(it)
        }

    override fun wrapTypeIfRequired(baseType: ClassId): ClassId =
        if (baseType.isAccessibleFrom(testClassPackageName)) baseType else objectClassId

    // utils

    private fun classRefOrNull(type: ClassId, expr: CgExpression): ClassId? {
        if (type == classClassId && expr is CgGetClass) return expr.classId

        if (type == classClassId && expr is CgExecutableCall && expr.executableId == forName) {
            val name = (expr.arguments.getOrNull(0) as? CgLiteral)?.value as? String

            if (name != null) {
                return BuiltinClassId.getBuiltinClassByNameOrNull(name) ?: ClassId(name)
            }
        }

        return null
    }

    private fun guardEnumConstantAccess(access: CgEnumConstantAccess): ExpressionWithType {
        val (enumClass, constant) = access

        return if (enumClass.isAccessibleFrom(testClassPackageName)) {
            ExpressionWithType(enumClass, access)
        } else {
            val enumClassVariable = newVar(classCgClassId) {
                classClassId[forName](enumClass.name)
            }

            ExpressionWithType(objectClassId, utilsClassId[getEnumConstantByName](enumClassVariable, constant))
        }
    }

    private fun guardArrayAllocation(allocation: CgAllocateArray): ExpressionWithType {
        return guardArrayCreation(allocation.type, allocation.size, allocation)
    }

    private fun guardArrayInitializer(initializer: CgArrayInitializer): ExpressionWithType {
        return guardArrayCreation(initializer.type, initializer.size, initializer)
    }

    private fun guardArrayCreation(arrayType: ClassId, arraySize: Int, initialization: CgExpression): ExpressionWithType {
        // TODO: check if this is the right way to check array type accessibility
        return if (arrayType.isAccessibleFrom(testClassPackageName)) {
            ExpressionWithType(arrayType, initialization)
        } else {
            ExpressionWithType(
                objectArrayClassId,
                utilsClassId[createArray](arrayType.elementClassId!!.name, arraySize)
            )
        }
    }

    private val ExecutableId.kotlinFunction: KFunction<*>?
        get() {
            return when (val executable = this.executable) {
                is Method -> executable.kotlinFunction
                is Constructor<*> -> executable.kotlinFunction
                else -> error("Unknown Executable type: ${this::class}")
            }
        }

    private fun guardExecutableCall(baseType: ClassId, call: CgExecutableCall): ExpressionWithType {
        // TODO: SAT-1210 support generics so that we wouldn't need to obtain kotlinFunction
        // TODO: in order to check whether we are working with a TypeVariable or not
//        val returnType = runCatching { call.executableId.kotlinFunction }.getOrNull()?.returnType?.javaType

        if (call.executableId != mockMethodId) return guardExpression(baseType, call)

        // call represents a call to mock() method
        val wrappedType = wrapTypeIfRequired(baseType)
        return ExpressionWithType(wrappedType, call)
    }

    override fun guardExpression(baseType: ClassId, expression: CgExpression): ExpressionWithType {
        val type: ClassId
        val expr: CgExpression

        val typeAccessible = baseType.isAccessibleFrom(testClassPackageName)

        when {
            expression.type isSubtypeOf baseType && typeAccessible -> {
                type = baseType
                expr = expression
            }
            expression.type isSubtypeOf baseType && !typeAccessible -> {
                type = if (expression.type.isArray) objectArrayClassId else objectClassId
                expr = expression
            }
            expression.type isNotSubtypeOf baseType && typeAccessible -> {
                // consider util methods getField and getStaticField
                // TODO should we consider another cases?
                val isGetFieldUtilMethod = (expression is CgMethodCall && expression.executableId.isGetFieldUtilMethod)
                val shouldCastBeSafety = expression == nullLiteral() || isGetFieldUtilMethod

                expr = typeCast(baseType, expression, shouldCastBeSafety)
                type = expr.type
            }
            expression.type isNotSubtypeOf baseType && !typeAccessible -> {
                type = if (expression.type.isArray) objectArrayClassId else objectClassId
                expr = if (expression is CgMethodCall && isUtil(expression.executableId)) {
                    CgErrorWrapper("${expression.executableId.name} failed", expression)
                } else {
                    expression
                }
            }
            else -> error("Impossible case")
        }

        return ExpressionWithType(type, expr)
    }
}
