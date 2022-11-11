package parser.visitors

import parser.ast.AstNode
import parser.ast.BaseTypeNode
import parser.ast.BinaryExpressionNode
import parser.ast.ClassDeclarationNode
import parser.ast.ComparisonBinaryOperatorNode
import parser.ast.ConstructorNode
import parser.ast.CustomTypeNode
import parser.ast.DummyNode
import parser.ast.FunctionDeclarationNode
import parser.ast.IdentifierNode
import parser.ast.MathBinaryOperatorNode
import parser.ast.MethodDeclarationNode
import parser.ast.NumericLiteralNode
import parser.ast.ParameterNode
import parser.ast.PropertyDeclarationNode

abstract class AbstractAstVisitor {

    fun accept(root: AstNode) {
        val shouldContinue = when (root) {
            is DummyNode -> visitDummyNode(root)
            is BaseTypeNode -> visitBaseTypeNode(root)
            is BinaryExpressionNode -> visitBinaryExpressionNode(root)
            is ClassDeclarationNode -> visitClassDeclarationNode(root)
            is ComparisonBinaryOperatorNode -> visitComparisonBinaryOperationNode(root)
            is ConstructorNode -> visitConstructorNode(root)
            is CustomTypeNode -> visitCustomTypeNode(root)
            is FunctionDeclarationNode -> visitFunctionDeclarationNode(root)
            is IdentifierNode -> visitIdentifierNode(root)
            is MathBinaryOperatorNode -> visitMathBinaryOperatorNode(root)
            is MethodDeclarationNode -> visitMethodDeclarationNode(root)
            is ParameterNode -> visitParameterNode(root)
            is PropertyDeclarationNode -> visitPropertyDeclarationNode(root)
            is NumericLiteralNode -> visitNumericLiteralNode(root)
            else -> throw IllegalStateException("No such AST node exists!")
        }
        if (shouldContinue) {
            for (child in root.children) accept(child)
        }
    }

    open fun visitDummyNode(node: DummyNode): Boolean = true

    open fun visitBaseTypeNode(node: BaseTypeNode): Boolean = true

    open fun visitBinaryExpressionNode(node: BinaryExpressionNode): Boolean = true

    open fun visitClassDeclarationNode(node: ClassDeclarationNode): Boolean = true

    open fun visitComparisonBinaryOperationNode(node: ComparisonBinaryOperatorNode): Boolean = true

    open fun visitConstructorNode(node: ConstructorNode): Boolean = true

    open fun visitCustomTypeNode(node: CustomTypeNode): Boolean = true

    open fun visitFunctionDeclarationNode(node: FunctionDeclarationNode): Boolean = true

    open fun visitIdentifierNode(node: IdentifierNode): Boolean = true

    open fun visitMathBinaryOperatorNode(node: MathBinaryOperatorNode): Boolean = true

    open fun visitMethodDeclarationNode(node: MethodDeclarationNode): Boolean = true

    open fun visitParameterNode(node: ParameterNode): Boolean = true

    open fun visitPropertyDeclarationNode(node: PropertyDeclarationNode): Boolean = true

    open fun visitNumericLiteralNode(node: NumericLiteralNode): Boolean = true
}