package parser.ast

import com.eclipsesource.v8.V8Object

class NumericLiteralNode(
    obj: V8Object,
): LiteralNode() {

    override val value: Any = obj.getString("text").toNumber()

    private fun String.toNumber(): Any =
        if (this.contains(".")) {
            this.toDoubleOrNull() ?: this.toBigDecimal()
        } else {
            this.toLongOrNull() ?: this.toBigInteger()
        }
}