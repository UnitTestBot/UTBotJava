package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getKind

class CustomTypeNode(
    obj: V8Object,
    typescript: V8Object
): TypeNode() {
    // TODO: WRONG, FIX!!!!
    override val stringTypeName = obj.getKind(typescript)
}