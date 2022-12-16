package org.utbot.language.ts.parser.visitors

import org.utbot.language.ts.parser.ast.ArrowFunctionNode
import org.utbot.language.ts.parser.ast.AstNode
import org.utbot.language.ts.parser.ast.BaseTypeNode
import org.utbot.language.ts.parser.ast.BinaryExpressionNode
import org.utbot.language.ts.parser.ast.CallExpressionNode
import org.utbot.language.ts.parser.ast.ClassDeclarationNode
import org.utbot.language.ts.parser.ast.ComparisonBinaryOperatorNode
import org.utbot.language.ts.parser.ast.ConstructorNode
import org.utbot.language.ts.parser.ast.CustomTypeNode
import org.utbot.language.ts.parser.ast.DummyNode
import org.utbot.language.ts.parser.ast.FunctionDeclarationNode
import org.utbot.language.ts.parser.ast.FunctionTypeNode
import org.utbot.language.ts.parser.ast.IdentifierNode
import org.utbot.language.ts.parser.ast.ImportDeclarationNode
import org.utbot.language.ts.parser.ast.MathBinaryOperatorNode
import org.utbot.language.ts.parser.ast.MethodDeclarationNode
import org.utbot.language.ts.parser.ast.NumericLiteralNode
import org.utbot.language.ts.parser.ast.ParameterNode
import org.utbot.language.ts.parser.ast.PropertyAccessExpressionNode
import org.utbot.language.ts.parser.ast.PropertyDeclarationNode
import org.utbot.language.ts.parser.ast.VariableDeclarationNode
import org.utbot.language.ts.parser.ast.VariableStatementNode

abstract class AbstractAstVisitor {

    fun accept(root: AstNode) {
        val shouldContinue = when (root) {
            is ArrowFunctionNode -> visitArrowFunctionNode(root)
            is DummyNode -> visitDummyNode(root)
            is BaseTypeNode -> visitBaseTypeNode(root)
            is BinaryExpressionNode -> visitBinaryExpressionNode(root)
            is CallExpressionNode -> visitCallExpressionNode(root)
            is ClassDeclarationNode -> visitClassDeclarationNode(root)
            is ComparisonBinaryOperatorNode -> visitComparisonBinaryOperationNode(root)
            is ConstructorNode -> visitConstructorNode(root)
            is CustomTypeNode -> visitCustomTypeNode(root)
            is FunctionDeclarationNode -> visitFunctionDeclarationNode(root)
            is IdentifierNode -> visitIdentifierNode(root)
            is ImportDeclarationNode -> visitImportDeclarationNode(root)
            is MathBinaryOperatorNode -> visitMathBinaryOperatorNode(root)
            is MethodDeclarationNode -> visitMethodDeclarationNode(root)
            is NumericLiteralNode -> visitNumericLiteralNode(root)
            is ParameterNode -> visitParameterNode(root)
            is PropertyDeclarationNode -> visitPropertyDeclarationNode(root)
            is PropertyAccessExpressionNode -> visitPropertyAccessExpressionNode(root)
            is FunctionTypeNode -> visitFunctionTypeNode(root)
            is VariableDeclarationNode -> visitVariableDeclarationNode(root)
            is VariableStatementNode -> visitVariableStatementNode(root)
            else -> throw IllegalStateException("No such AST node exists: $root")
        }
        if (shouldContinue) {
            for (child in root.children) accept(child)
        }
    }

    open fun visitArrowFunctionNode(node: ArrowFunctionNode): Boolean = true

    open fun visitDummyNode(node: DummyNode): Boolean = true

    open fun visitBaseTypeNode(node: BaseTypeNode): Boolean = true

    open fun visitBinaryExpressionNode(node: BinaryExpressionNode): Boolean = true

    open fun visitCallExpressionNode(node: CallExpressionNode): Boolean = true

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

    open fun visitImportDeclarationNode(node: ImportDeclarationNode): Boolean = true

    open fun visitPropertyAccessExpressionNode(node: PropertyAccessExpressionNode): Boolean = true

    open fun visitFunctionTypeNode(node: FunctionTypeNode): Boolean = true

    open fun visitVariableDeclarationNode(node: VariableDeclarationNode): Boolean = true

    open fun visitVariableStatementNode(node: VariableStatementNode): Boolean = true
}