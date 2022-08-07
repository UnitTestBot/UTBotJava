package org.utbot.framework.codegen.model.visitor

import org.utbot.framework.codegen.model.tree.*

interface CgVisitor<R> {
    fun visit(element: CgPythonRepr): R

    fun visit(element: CgElement): R

    fun visit(element: CgTestClassFile): R

    fun visit(element: CgTestClass): R

    fun visit(element: CgTestClassBody): R

    fun visit(element: CgStaticsRegion): R
    fun visit(element: CgSimpleRegion<*>): R
    fun visit(element: CgTestMethodCluster): R
    fun visit(element: CgExecutableUnderTestCluster): R

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
