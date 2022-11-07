package parser.ast

import com.eclipsesource.v8.V8Object

class FunctionDeclarationNode(
    obj: V8Object,
    typescript: V8Object
): FunctionNode(obj, typescript)