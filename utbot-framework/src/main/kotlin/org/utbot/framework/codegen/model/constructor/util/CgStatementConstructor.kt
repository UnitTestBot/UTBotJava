package org.utbot.framework.codegen.model.constructor.util

import fj.data.Either
import kotlinx.coroutines.runBlocking
import org.utbot.framework.codegen.model.constructor.builtin.forName
import org.utbot.framework.codegen.model.constructor.builtin.mockMethodId
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.tree.CgCallableAccessManager
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.codegen.model.util.buildExceptionHandler
import org.utbot.framework.codegen.model.util.isAccessibleFrom
import org.utbot.framework.codegen.model.util.nullLiteral
import org.utbot.framework.codegen.model.util.resolve
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.ext.findClass
import org.utbot.jcdb.api.ifArrayGetElementClass
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.kotlinFunction

interface CgStatementConstructor {
    fun newVar(baseType: CgClassType, baseName: String? = null, init: () -> CgExpression): CgVariable =
        newVar(baseType, model = null, baseName, isMutable = false, init)

    fun newVar(
        baseType: CgClassType,
        baseName: String? = null,
        isMutable: Boolean = false,
        init: () -> CgExpression
    ): CgVariable =
        newVar(baseType, model = null, baseName, isMutable = isMutable, init)

    fun newVar(
        baseType: CgClassType,
        model: UtModel? = null,
        baseName: String? = null,
        isMutable: Boolean = false,
        init: () -> CgExpression
    ): CgVariable = newVar(baseType, model, baseName, isMock = false, isMutable = isMutable, init)

    fun newVar(
        baseType: CgClassType,
        model: UtModel? = null,
        baseName: String? = null,
        isMock: Boolean = false,
        isMutable: Boolean = false,
        init: () -> CgExpression
    ): CgVariable

    fun createDeclarationForNewVarAndUpdateVariableScopeOrGetExistingVariable(
        baseType: CgClassType,
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

    fun declareParameter(type: CgClassType, name: String): CgVariable = declareVariable(type, name)

    fun declareVariable(type: CgClassType, name: String): CgVariable

    fun guardExpression(baseType: CgClassType, expression: CgExpression): ExpressionWithType

    fun wrapTypeIfRequired(baseType: CgClassType): CgClassType
}

internal class CgStatementConstructorImpl(context: CgContext) :
    CgStatementConstructor,
    CgContextOwner by context,
    CgCallableAccessManager by CgComponents.getCallableAccessManagerBy(context) {

    private val nameGenerator = CgComponents.getNameGeneratorBy(context)

    override fun createDeclarationForNewVarAndUpdateVariableScopeOrGetExistingVariable(
        baseType: CgClassType,
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
                val base = classRef.simpleName.decapitalize()
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
        baseType: CgClassType,
        model: UtModel?,
        baseName: String?,
        isMock: Boolean,
        isMutable: Boolean,
        init: () -> CgExpression
    ): CgVariable {
        val declarationOrVar: Either<CgDeclaration, CgVariable> =
            createDeclarationForNewVarAndUpdateVariableScopeOrGetExistingVariable(
                baseType,
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

    override fun tryBlock(init: () -> Unit): CgTryCatch = tryBlock(init, null)

    override fun tryBlock(init: () -> Unit, resources: List<CgDeclaration>?): CgTryCatch =
        buildTryCatch {
            statements = block(init)
            this.resources = resources
        }

    override fun CgTryCatch.catch(exception: ClassId, init: (CgVariable) -> Unit): CgTryCatch {
        val newHandler = buildExceptionHandler {
            val e = declareVariable(exception.type(false), nameGenerator.variableName(exception.simpleName.decapitalize()))
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
            CgAnonymousFunction(CgClassType(type), paramDeclarations, block(body))
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

    override fun declareVariable(type: CgClassType, name: String): CgVariable =
        CgVariable(name, type).also {
            updateVariableScope(it)
        }

    override fun wrapTypeIfRequired(baseType: CgClassType): CgClassType =
        if (baseType.classId.isAccessibleFrom(testClassPackageName)) baseType else CgClassType(objectClassId)

    // utils

    private fun classRefOrNull(type: CgClassType, expr: CgExpression): ClassId? {
        val classId = type.classId
        if (classId == Class::class.id && expr is CgGetClass) return expr.classId

        if (classId == Class::class.id && expr is CgExecutableCall && expr.executableId == forName) {
            val name = (expr.arguments.getOrNull(0) as? CgLiteral)?.value as? String

            if (name != null) {
                return runBlocking { utContext.classpath.findClass(name) }
            }
        }

        return null
    }

    private fun guardEnumConstantAccess(access: CgEnumConstantAccess): ExpressionWithType {
        val (enumClass, constant) = access

        return if (enumClass.isAccessibleFrom(testClassPackageName)) {
            ExpressionWithType(enumClass.type(), access)
        } else {
            val enumClassVariable = newVar(classClassId.type(false)) {
                Class::class.id[forName](enumClass.name)
            }

            ExpressionWithType(objectClassId.type(true), testClassThisInstance[getEnumConstantByName](enumClassVariable, constant))
        }
    }

    private fun guardArrayAllocation(allocation: CgAllocateArray): ExpressionWithType {
        return guardArrayCreation(allocation.type, allocation.size, allocation)
    }

    private fun guardArrayInitializer(initializer: CgArrayInitializer): ExpressionWithType {
        return guardArrayCreation(initializer.type, initializer.size, initializer)
    }

    private fun guardArrayCreation(arrayType: CgClassType, arraySize: Int, initialization: CgExpression): ExpressionWithType {
        // TODO: check if this is the right way to check array type accessibility
        return if (arrayType.classId.isAccessibleFrom(testClassPackageName)) {
            ExpressionWithType(arrayType, initialization)
        } else {
            ExpressionWithType(
                CgClassType(objectArrayClassId),
                testClassThisInstance[createArray](arrayType.classId.ifArrayGetElementClass()!!.name, arraySize)
            )
        }
    }

    private val ExecutableId.kotlinFunction: KFunction<*>?
        get() = with(reflection) {
            return when (val executable = executable) {
                is Method -> executable.kotlinFunction
                is Constructor<*> -> executable.kotlinFunction
                else -> error("Unknown Executable type: ${this::class}")
            }
        }

    private fun guardExecutableCall(baseType: CgClassType, call: CgExecutableCall): ExpressionWithType {
        // TODO: SAT-1210 support generics so that we wouldn't need to obtain kotlinFunction
        // TODO: in order to check whether we are working with a TypeVariable or not
//        val returnType = runCatching { call.executableId.kotlinFunction }.getOrNull()?.returnType?.javaType

        if (call.executableId.methodId != mockMethodId) return guardExpression(baseType, call)

        // call represents a call to mock() method
        val wrappedType = wrapTypeIfRequired(baseType)
        return ExpressionWithType(wrappedType, call)
    }

    override fun guardExpression(baseType: CgClassType, expression: CgExpression): ExpressionWithType {
        val type: CgClassType
        val expr: CgExpression

        val typeAccessible = baseType.classId.isAccessibleFrom(testClassPackageName)

        when {
            expression.type.classId blockingIsSubtypeOf baseType.classId && typeAccessible -> {
                type = baseType
                expr = expression
            }
            expression.type.classId blockingIsSubtypeOf baseType.classId && !typeAccessible -> {
                type = if (expression.type.classId.isArray) CgClassType(objectArrayClassId, isNullable = expression.type.isNullable) else CgClassType(objectClassId)
                expr = expression
            }
            expression.type.classId blockingIsNotSubtypeOf baseType.classId && typeAccessible -> {
                // consider util methods getField and getStaticField
                // TODO should we consider another cases?
                val isGetFieldUtilMethod = (expression is CgMethodCall && expression.executableId.isGetFieldUtilMethod)
                val shouldCastBeSafety = expression == nullLiteral() || isGetFieldUtilMethod

                type = baseType
                expr = typeCast(baseType, expression, shouldCastBeSafety)
            }
            expression.type.classId blockingIsNotSubtypeOf baseType.classId && !typeAccessible -> {
                type = if (expression.type.classId.isArray) CgClassType(objectArrayClassId, isNullable = expression.type.isNullable) else CgClassType(objectClassId, isNullable = expression.type.isNullable)
                expr = if (expression is CgMethodCall && expression.executableId.isUtil) {
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
