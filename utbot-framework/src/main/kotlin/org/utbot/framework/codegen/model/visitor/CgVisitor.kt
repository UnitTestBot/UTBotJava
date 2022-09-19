package org.utbot.framework.codegen.model.visitor

import org.utbot.framework.codegen.model.tree.AbstractCgClass
import org.utbot.framework.codegen.model.tree.AbstractCgClassBody
import org.utbot.framework.codegen.model.tree.AbstractCgClassFile
import org.utbot.framework.codegen.model.tree.CgAbstractFieldAccess
import org.utbot.framework.codegen.model.tree.CgAbstractMultilineComment
import org.utbot.framework.codegen.model.tree.CgAllocateArray
import org.utbot.framework.codegen.model.tree.CgAllocateInitializedArray
import org.utbot.framework.codegen.model.tree.CgAnonymousFunction
import org.utbot.framework.codegen.model.tree.CgArrayAnnotationArgument
import org.utbot.framework.codegen.model.tree.CgArrayElementAccess
import org.utbot.framework.codegen.model.tree.CgArrayInitializer
import org.utbot.framework.codegen.model.tree.CgAssignment
import org.utbot.framework.codegen.model.tree.CgAuxiliaryClass
import org.utbot.framework.codegen.model.tree.CgBreakStatement
import org.utbot.framework.codegen.model.tree.CgComment
import org.utbot.framework.codegen.model.tree.CgCommentedAnnotation
import org.utbot.framework.codegen.model.tree.CgComparison
import org.utbot.framework.codegen.model.tree.CgConstructorCall
import org.utbot.framework.codegen.model.tree.CgContinueStatement
import org.utbot.framework.codegen.model.tree.CgDeclaration
import org.utbot.framework.codegen.model.tree.CgDecrement
import org.utbot.framework.codegen.model.tree.CgDoWhileLoop
import org.utbot.framework.codegen.model.tree.CgDocClassLinkStmt
import org.utbot.framework.codegen.model.tree.CgDocCodeStmt
import org.utbot.framework.codegen.model.tree.CgDocMethodLinkStmt
import org.utbot.framework.codegen.model.tree.CgDocPreTagStatement
import org.utbot.framework.codegen.model.tree.CgCustomTagStatement
import org.utbot.framework.codegen.model.tree.CgDocRegularStmt
import org.utbot.framework.codegen.model.tree.CgDocumentationComment
import org.utbot.framework.codegen.model.tree.CgElement
import org.utbot.framework.codegen.model.tree.CgEmptyLine
import org.utbot.framework.codegen.model.tree.CgEnumConstantAccess
import org.utbot.framework.codegen.model.tree.CgEqualTo
import org.utbot.framework.codegen.model.tree.CgErrorTestMethod
import org.utbot.framework.codegen.model.tree.CgErrorWrapper
import org.utbot.framework.codegen.model.tree.CgExecutableCall
import org.utbot.framework.codegen.model.tree.CgExecutableUnderTestCluster
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgFieldAccess
import org.utbot.framework.codegen.model.tree.CgForLoop
import org.utbot.framework.codegen.model.tree.CgGetJavaClass
import org.utbot.framework.codegen.model.tree.CgGetKotlinClass
import org.utbot.framework.codegen.model.tree.CgGetLength
import org.utbot.framework.codegen.model.tree.CgGreaterThan
import org.utbot.framework.codegen.model.tree.CgIfStatement
import org.utbot.framework.codegen.model.tree.CgIncrement
import org.utbot.framework.codegen.model.tree.CgInnerBlock
import org.utbot.framework.codegen.model.tree.CgIsInstance
import org.utbot.framework.codegen.model.tree.CgLessThan
import org.utbot.framework.codegen.model.tree.CgLiteral
import org.utbot.framework.codegen.model.tree.CgLogicalAnd
import org.utbot.framework.codegen.model.tree.CgLogicalOr
import org.utbot.framework.codegen.model.tree.CgLoop
import org.utbot.framework.codegen.model.tree.CgMethod
import org.utbot.framework.codegen.model.tree.CgMethodCall
import org.utbot.framework.codegen.model.tree.CgMultilineComment
import org.utbot.framework.codegen.model.tree.CgMultipleArgsAnnotation
import org.utbot.framework.codegen.model.tree.CgNamedAnnotationArgument
import org.utbot.framework.codegen.model.tree.CgNonStaticRunnable
import org.utbot.framework.codegen.model.tree.CgNotNullAssertion
import org.utbot.framework.codegen.model.tree.CgParameterDeclaration
import org.utbot.framework.codegen.model.tree.CgParameterizedTestDataProviderMethod
import org.utbot.framework.codegen.model.tree.CgRegularClass
import org.utbot.framework.codegen.model.tree.CgRegularClassBody
import org.utbot.framework.codegen.model.tree.CgRegularClassFile
import org.utbot.framework.codegen.model.tree.CgReturnStatement
import org.utbot.framework.codegen.model.tree.CgSimpleRegion
import org.utbot.framework.codegen.model.tree.CgSingleArgAnnotation
import org.utbot.framework.codegen.model.tree.CgSingleLineComment
import org.utbot.framework.codegen.model.tree.CgSpread
import org.utbot.framework.codegen.model.tree.CgStatement
import org.utbot.framework.codegen.model.tree.CgStatementExecutableCall
import org.utbot.framework.codegen.model.tree.CgStaticFieldAccess
import org.utbot.framework.codegen.model.tree.CgStaticRunnable
import org.utbot.framework.codegen.model.tree.CgStaticsRegion
import org.utbot.framework.codegen.model.tree.CgSwitchCase
import org.utbot.framework.codegen.model.tree.CgSwitchCaseLabel
import org.utbot.framework.codegen.model.tree.CgTestClass
import org.utbot.framework.codegen.model.tree.CgTestClassBody
import org.utbot.framework.codegen.model.tree.CgTestClassFile
import org.utbot.framework.codegen.model.tree.CgTestMethod
import org.utbot.framework.codegen.model.tree.CgTestMethodCluster
import org.utbot.framework.codegen.model.tree.CgThisInstance
import org.utbot.framework.codegen.model.tree.CgThrowStatement
import org.utbot.framework.codegen.model.tree.CgTripleSlashMultilineComment
import org.utbot.framework.codegen.model.tree.CgTryCatch
import org.utbot.framework.codegen.model.tree.CgTypeCast
import org.utbot.framework.codegen.model.tree.CgUtilMethod
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.tree.CgWhileLoop

interface CgVisitor<R> {
    fun visit(element: CgElement): R

    fun visit(element: AbstractCgClassFile<*>): R
    fun visit(element: CgRegularClassFile): R
    fun visit(element: CgTestClassFile): R

    fun visit(element: AbstractCgClass<*>): R
    fun visit(element: CgRegularClass): R
    fun visit(element: CgTestClass): R

    fun visit(element: AbstractCgClassBody): R
    fun visit(element: CgRegularClassBody): R
    fun visit(element: CgTestClassBody): R

    fun visit(element: CgStaticsRegion): R
    fun visit(element: CgSimpleRegion<*>): R
    fun visit(element: CgTestMethodCluster): R
    fun visit(element: CgExecutableUnderTestCluster): R

    fun visit(element: CgAuxiliaryClass): R
    fun visit(element: CgUtilMethod): R

    // Methods
    fun visit(element: CgMethod): R
    fun visit(element: CgTestMethod): R
    fun visit(element: CgErrorTestMethod): R
    fun visit(element: CgParameterizedTestDataProviderMethod): R

    // Annotations
    fun visit(element: CgCommentedAnnotation): R
    fun visit(element: CgSingleArgAnnotation): R
    fun visit(element: CgMultipleArgsAnnotation): R
    fun visit(element: CgNamedAnnotationArgument): R
    fun visit(element: CgArrayAnnotationArgument): R

    // Comments
    fun visit(element: CgComment): R
    fun visit(element: CgSingleLineComment): R
    fun visit(element: CgAbstractMultilineComment): R
    fun visit(element: CgTripleSlashMultilineComment): R
    fun visit(element: CgMultilineComment): R
    fun visit(element: CgDocumentationComment): R

    // Comment statements
    fun visit(element: CgDocPreTagStatement): R
    fun visit(element: CgCustomTagStatement): R
    fun visit(element: CgDocCodeStmt): R
    fun visit(element: CgDocRegularStmt): R
    fun visit(element: CgDocClassLinkStmt): R
    fun visit(element: CgDocMethodLinkStmt): R

    // Any block
    // IMPORTANT: there is no line separator in the end by default
    // because some blocks have it (like loops) but some do not (like try .. catch, if .. else etc)
    fun visit(block: List<CgStatement>, printNextLine: Boolean = false): R

    // Anonymous function (lambda)
    fun visit(element: CgAnonymousFunction): R

    // Return statement
    fun visit(element: CgReturnStatement): R

    // Array element access
    fun visit(element: CgArrayElementAccess): R

    // Loop conditions
    fun visit(element: CgComparison): R
    fun visit(element: CgLessThan): R
    fun visit(element: CgGreaterThan): R
    fun visit(element: CgEqualTo): R

    // Increment and decrement
    fun visit(element: CgIncrement): R
    fun visit(element: CgDecrement): R

    fun visit(element: CgErrorWrapper): R

    // Try-catch
    fun visit(element: CgTryCatch): R

    //Simple block
    fun visit(element: CgInnerBlock): R

    // Loops
    fun visit(element: CgLoop): R
    fun visit(element: CgForLoop): R
    fun visit(element: CgWhileLoop): R
    fun visit(element: CgDoWhileLoop): R

    // Control statements
    fun visit(element: CgBreakStatement): R
    fun visit(element: CgContinueStatement): R

    // Variable declaration
    fun visit(element: CgDeclaration): R

    // Variable assignment
    fun visit(element: CgAssignment): R

    // Expressions
    fun visit(element: CgExpression): R

    // Type cast
    fun visit(element: CgTypeCast): R

    // isInstance check
    fun visit(element: CgIsInstance): R

    // This instance
    fun visit(element: CgThisInstance): R

    // Variables
    fun visit(element: CgVariable): R

    // Not-null assertion

    fun visit(element: CgNotNullAssertion): R

    // Method parameters
    fun visit(element: CgParameterDeclaration): R

    // Primitive and String literals
    fun visit(element: CgLiteral): R

    // Non-static runnable like this::toString or (new Object())::toString etc
    fun visit(element: CgNonStaticRunnable): R
    // Static runnable like Random::nextRandomInt etc
    fun visit(element: CgStaticRunnable): R

    // Array allocation
    fun visit(element: CgAllocateArray): R
    fun visit(element: CgAllocateInitializedArray): R
    fun visit(element: CgArrayInitializer): R

    // Spread operator
    fun visit(element: CgSpread): R

    // Enum constant
    fun visit(element: CgEnumConstantAccess): R

    // Property access
    fun visit(element: CgAbstractFieldAccess): R
    fun visit(element: CgFieldAccess): R
    fun visit(element: CgStaticFieldAccess): R

    // Conditional statement

    fun visit(element: CgIfStatement): R
    fun visit(element: CgSwitchCaseLabel): R
    fun visit(element: CgSwitchCase): R

    // Binary logical operators

    fun visit(element: CgLogicalAnd): R
    fun visit(element: CgLogicalOr): R

    // Acquisition of array length, e.g. args.length
    fun visit(element: CgGetLength): R

    // Acquisition of java or kotlin class, e.g. MyClass.class in Java, MyClass::class.java in Kotlin or MyClass::class for Kotlin classes
    fun visit(element: CgGetJavaClass): R
    fun visit(element: CgGetKotlinClass): R

    // Executable calls
    fun visit(element: CgStatementExecutableCall): R
    fun visit(element: CgExecutableCall): R
    fun visit(element: CgConstructorCall): R
    fun visit(element: CgMethodCall): R

    // Throw statement
    fun visit(element: CgThrowStatement): R

    // Empty line
    fun visit(element: CgEmptyLine): R
}
