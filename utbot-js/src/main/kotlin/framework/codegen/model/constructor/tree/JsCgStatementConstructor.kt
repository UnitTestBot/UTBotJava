package framework.codegen.model.constructor.tree

import fj.data.Either
import framework.codegen.model.constructor.util.plus
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgAnnotation
import org.utbot.framework.codegen.domain.models.CgAnonymousFunction
import org.utbot.framework.codegen.domain.models.CgComment
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgEmptyLine
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgIfStatement
import org.utbot.framework.codegen.domain.models.CgInnerBlock
import org.utbot.framework.codegen.domain.models.CgIsInstance
import org.utbot.framework.codegen.domain.models.CgLogicalAnd
import org.utbot.framework.codegen.domain.models.CgLogicalOr
import org.utbot.framework.codegen.domain.models.CgMultilineComment
import org.utbot.framework.codegen.domain.models.CgMultipleArgsAnnotation
import org.utbot.framework.codegen.domain.models.CgNamedAnnotationArgument
import org.utbot.framework.codegen.domain.models.CgParameterDeclaration
import org.utbot.framework.codegen.domain.models.CgReturnStatement
import org.utbot.framework.codegen.domain.models.CgSingleArgAnnotation
import org.utbot.framework.codegen.domain.models.CgSingleLineComment
import org.utbot.framework.codegen.domain.models.CgThrowStatement
import org.utbot.framework.codegen.domain.models.CgTryCatch
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.tree.CgComponents
import org.utbot.framework.codegen.tree.CgForEachLoopBuilder
import org.utbot.framework.codegen.tree.CgForLoopBuilder
import org.utbot.framework.codegen.tree.CgStatementConstructor
import org.utbot.framework.codegen.tree.ExpressionWithType
import org.utbot.framework.codegen.tree.buildAssignment
import org.utbot.framework.codegen.tree.buildDeclaration
import org.utbot.framework.codegen.tree.buildDoWhileLoop
import org.utbot.framework.codegen.tree.buildExceptionHandler
import org.utbot.framework.codegen.tree.buildForLoop
import org.utbot.framework.codegen.tree.buildTryCatch
import org.utbot.framework.codegen.tree.buildWhileLoop
import org.utbot.framework.codegen.util.resolve
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtModel

class JsCgStatementConstructor(context: CgContext) :
    CgStatementConstructor,
    CgContextOwner by context,
    CgCallableAccessManager by CgComponents.getCallableAccessManagerBy(context) {

    private val nameGenerator = CgComponents.getNameGeneratorBy(context)

    override fun newVar(
        baseType: ClassId,
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

    override fun createDeclarationForNewVarAndUpdateVariableScopeOrGetExistingVariable(
        baseType: ClassId,
        model: UtModel?,
        baseName: String?,
        isMock: Boolean,
        isMutableVar: Boolean,
        init: () -> CgExpression
    ): Either<CgDeclaration, CgVariable> {

        val baseExpr = init()

        val name = nameGenerator.variableName(baseType, baseName, isMock)

        // TODO SEVERE: here was import section for CgClassId. Implement it
//        importIfNeeded(baseType)
//        if ((baseType as JsClassId).name != "undefined") {
//            importedClasses += baseType
//        }

        val declaration = buildDeclaration {
            variableType = baseType
            variableName = name
            initializer = baseExpr
            isMutable = isMutableVar
        }

        updateVariableScope(declaration.variable, model)

        return Either.left(declaration)
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

    override fun ifStatement(
        condition: CgExpression,
        trueBranch: () -> Unit,
        falseBranch: (() -> Unit)?
    ): CgIfStatement {
        val trueBranchBlock = block(trueBranch)
        val falseBranchBlock = falseBranch?.let { block(it) }
        return CgIfStatement(condition, trueBranchBlock, falseBranchBlock).also {
            currentBlock += it
        }
    }

    override fun forLoop(init: CgForLoopBuilder.() -> Unit) {
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

    override fun forEachLoop(init: CgForEachLoopBuilder.() -> Unit) {
        throw UnsupportedOperationException("JavaScript does not have forEach loops")
    }

    override fun getClassOf(classId: ClassId): CgExpression {
        TODO("Not yet implemented")
    }

    override fun createFieldVariable(fieldId: FieldId): CgVariable {
        TODO("Not yet implemented")
    }

    override fun createExecutableVariable(executableId: ExecutableId, arguments: List<CgExpression>): CgVariable {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    // TODO MINOR: check whether js has inner blocks
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

    override fun addAnnotation(classId: ClassId, argument: Any?, target: AnnotationTarget): CgAnnotation {
        val annotation = CgSingleArgAnnotation(classId, argument.resolve())
        addAnnotation(annotation)
        return annotation
    }

    override fun addAnnotation(
        classId: ClassId,
        namedArguments: List<CgNamedAnnotationArgument>,
        target: AnnotationTarget,
        ): CgAnnotation {
        val annotation = CgMultipleArgsAnnotation(classId, namedArguments.toMutableList(), target)
        addAnnotation(annotation)
        return annotation
    }

    override fun addAnnotation(
        classId: ClassId,
        buildArguments: MutableList<Pair<String, CgExpression>>.() -> Unit,
        target: AnnotationTarget,
    ): CgAnnotation {
        val arguments = mutableListOf<Pair<String, CgExpression>>()
            .apply(buildArguments)
            .map { (name, value) -> CgNamedAnnotationArgument(name, value) }
        val annotation = CgMultipleArgsAnnotation(classId, arguments.toMutableList(), target)
        addAnnotation(annotation)
        return annotation
    }

    private fun addAnnotation(annotation: CgAnnotation) {
        when (annotation.target) {
            AnnotationTarget.Method -> collectedMethodAnnotations.add(annotation)
            AnnotationTarget.Class,
            AnnotationTarget.Field -> error("Such annotations are not supported in JavasCRIPT")
        }

        importIfNeeded(annotation.classId)
    }

    override fun returnStatement(expression: () -> CgExpression) {
        currentBlock += CgReturnStatement(expression())
    }

    override fun throwStatement(exception: () -> CgExpression): CgThrowStatement =
        CgThrowStatement(exception()).also { currentBlock += it }

    override fun emptyLine() {
        currentBlock += CgEmptyLine
    }

    override fun emptyLineIfNeeded() {
        val lastStatement = currentBlock.lastOrNull() ?: return
        if (lastStatement is CgEmptyLine) return
        emptyLine()
    }

    override fun declareVariable(type: ClassId, name: String): CgVariable =
        CgVariable(name, type).also {
            updateVariableScope(it)
        }

    // TODO SEVERE: think about these 2 functions
    override fun guardExpression(baseType: ClassId, expression: CgExpression): ExpressionWithType =
        ExpressionWithType(baseType, expression)

    override fun wrapTypeIfRequired(baseType: ClassId): ClassId = baseType
}