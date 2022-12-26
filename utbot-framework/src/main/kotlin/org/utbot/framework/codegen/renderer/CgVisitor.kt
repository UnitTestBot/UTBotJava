package org.utbot.framework.codegen.renderer

import org.utbot.framework.codegen.domain.models.CgClassFile
import org.utbot.framework.codegen.domain.models.CgAbstractFieldAccess
import org.utbot.framework.codegen.domain.models.CgAbstractMultilineComment
import org.utbot.framework.codegen.domain.models.CgAllocateArray
import org.utbot.framework.codegen.domain.models.CgAllocateInitializedArray
import org.utbot.framework.codegen.domain.models.CgAnonymousFunction
import org.utbot.framework.codegen.domain.models.CgArrayAnnotationArgument
import org.utbot.framework.codegen.domain.models.CgArrayElementAccess
import org.utbot.framework.codegen.domain.models.CgArrayInitializer
import org.utbot.framework.codegen.domain.models.CgAssignment
import org.utbot.framework.codegen.domain.models.CgAuxiliaryClass
import org.utbot.framework.codegen.domain.models.CgBreakStatement
import org.utbot.framework.codegen.domain.models.CgComment
import org.utbot.framework.codegen.domain.models.CgCommentedAnnotation
import org.utbot.framework.codegen.domain.models.CgComparison
import org.utbot.framework.codegen.domain.models.CgConstructorCall
import org.utbot.framework.codegen.domain.models.CgContinueStatement
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgDecrement
import org.utbot.framework.codegen.domain.models.CgDoWhileLoop
import org.utbot.framework.codegen.domain.models.CgDocClassLinkStmt
import org.utbot.framework.codegen.domain.models.CgDocCodeStmt
import org.utbot.framework.codegen.domain.models.CgDocMethodLinkStmt
import org.utbot.framework.codegen.domain.models.CgDocPreTagStatement
import org.utbot.framework.codegen.domain.models.CgCustomTagStatement
import org.utbot.framework.codegen.domain.models.CgDocRegularStmt
import org.utbot.framework.codegen.domain.models.CgDocumentationComment
import org.utbot.framework.codegen.domain.models.CgElement
import org.utbot.framework.codegen.domain.models.CgEmptyLine
import org.utbot.framework.codegen.domain.models.CgEnumConstantAccess
import org.utbot.framework.codegen.domain.models.CgEqualTo
import org.utbot.framework.codegen.domain.models.CgErrorTestMethod
import org.utbot.framework.codegen.domain.models.CgErrorWrapper
import org.utbot.framework.codegen.domain.models.CgExecutableCall
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgFieldAccess
import org.utbot.framework.codegen.domain.models.CgForEachLoop
import org.utbot.framework.codegen.domain.models.CgForLoop
import org.utbot.framework.codegen.domain.models.CgGetJavaClass
import org.utbot.framework.codegen.domain.models.CgGetKotlinClass
import org.utbot.framework.codegen.domain.models.CgGetLength
import org.utbot.framework.codegen.domain.models.CgGreaterThan
import org.utbot.framework.codegen.domain.models.CgIfStatement
import org.utbot.framework.codegen.domain.models.CgIncrement
import org.utbot.framework.codegen.domain.models.CgInnerBlock
import org.utbot.framework.codegen.domain.models.CgIsInstance
import org.utbot.framework.codegen.domain.models.CgLessThan
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgLogicalAnd
import org.utbot.framework.codegen.domain.models.CgLogicalOr
import org.utbot.framework.codegen.domain.models.CgLoop
import org.utbot.framework.codegen.domain.models.CgMethod
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgMultilineComment
import org.utbot.framework.codegen.domain.models.CgMultipleArgsAnnotation
import org.utbot.framework.codegen.domain.models.CgNamedAnnotationArgument
import org.utbot.framework.codegen.domain.models.CgNonStaticRunnable
import org.utbot.framework.codegen.domain.models.CgNotNullAssertion
import org.utbot.framework.codegen.domain.models.CgParameterDeclaration
import org.utbot.framework.codegen.domain.models.CgParameterizedTestDataProviderMethod
import org.utbot.framework.codegen.domain.models.CgReturnStatement
import org.utbot.framework.codegen.domain.models.CgSimpleRegion
import org.utbot.framework.codegen.domain.models.CgSingleArgAnnotation
import org.utbot.framework.codegen.domain.models.CgSingleLineComment
import org.utbot.framework.codegen.domain.models.CgSpread
import org.utbot.framework.codegen.domain.models.CgStatement
import org.utbot.framework.codegen.domain.models.CgStatementExecutableCall
import org.utbot.framework.codegen.domain.models.CgStaticFieldAccess
import org.utbot.framework.codegen.domain.models.CgStaticRunnable
import org.utbot.framework.codegen.domain.models.CgStaticsRegion
import org.utbot.framework.codegen.domain.models.CgSwitchCase
import org.utbot.framework.codegen.domain.models.CgSwitchCaseLabel
import org.utbot.framework.codegen.domain.models.CgClass
import org.utbot.framework.codegen.domain.models.CgClassBody
import org.utbot.framework.codegen.domain.models.CgDocRegularLineStmt
import org.utbot.framework.codegen.domain.models.CgFormattedString
import org.utbot.framework.codegen.domain.models.CgNestedClassesRegion
import org.utbot.framework.codegen.domain.models.CgTestMethod
import org.utbot.framework.codegen.domain.models.CgTestMethodCluster
import org.utbot.framework.codegen.domain.models.CgThisInstance
import org.utbot.framework.codegen.domain.models.CgThrowStatement
import org.utbot.framework.codegen.domain.models.CgTripleSlashMultilineComment
import org.utbot.framework.codegen.domain.models.CgTryCatch
import org.utbot.framework.codegen.domain.models.CgTypeCast
import org.utbot.framework.codegen.domain.models.CgUtilMethod
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.domain.models.CgWhileLoop

interface CgVisitor<R> {
    fun visit(element: CgElement): R

    fun visit(element: CgClassFile): R

    fun visit(element: CgClass): R

    fun visit(element: CgClassBody): R

    fun visit(element: CgStaticsRegion): R

    fun visit(element: CgNestedClassesRegion<*>): R
    fun visit(element: CgSimpleRegion<*>): R
    fun visit(element: CgTestMethodCluster): R
    fun visit(element: CgMethodsCluster): R

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
    fun visit(element: CgDocRegularLineStmt): R
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

    // Formatted string
    fun visit(element: CgFormattedString): R

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

    fun visit(element: CgForEachLoop): R
}
