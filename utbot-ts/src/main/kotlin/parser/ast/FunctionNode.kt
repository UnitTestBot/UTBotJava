package parser.ast

abstract class FunctionNode(): AstNode() {

    abstract val name: Lazy<String>

    abstract val parameters: List<ParameterNode>

    abstract val body: List<AstNode>

   abstract val returnType: Lazy<TypeNode>
}