package org.utbot.instrumentation.instrumentation.et

// TODO: document this

sealed class InstructionExecution {
    abstract fun treeFormat(buffer: StringBuilder, prefix: String, childPrefix: String)
}

@DslMarker
annotation class ExTraceDsl

@ExTraceDsl
data class FunctionCall constructor(
    val signature: String,
    private val instructions: MutableList<InstructionExecution> = mutableListOf()
) : InstructionExecution() {
    fun addInstruction(instructionExecution: InstructionExecution) {
        instructions.add(instructionExecution)
    }

    override fun treeFormat(buffer: StringBuilder, prefix: String, childPrefix: String) {
        buffer.append(prefix + "$signature\n")
        val it = instructions.iterator()
        while (it.hasNext()) {
            val nxt = it.next()
            if (it.hasNext()) {
                nxt.treeFormat(buffer, childPrefix + "├── ", childPrefix + "│   ") // VM option: -Dfile.encoding=UTF8
            } else {
                nxt.treeFormat(buffer, childPrefix + "└── ", childPrefix + "    ") // VM option: -Dfile.encoding=UTF8
            }
        }
    }

    override fun toString(): String {
        val buffer = StringBuilder()
        treeFormat(buffer, "", "")
        return buffer.toString()
    }
}

object Pass : InstructionExecution() {
    override fun treeFormat(buffer: StringBuilder, prefix: String, childPrefix: String) {
        buffer.append(prefix + "pass" + "\n")
    }
}

fun FunctionCall.implThr() {
    addInstruction(ImplicitThrow)
}


object ImplicitThrow : InstructionExecution() {
    override fun treeFormat(buffer: StringBuilder, prefix: String, childPrefix: String) {
        buffer.append(prefix + "implicit throw" + "\n")
    }
}

fun FunctionCall.explThr() {
    addInstruction(ExplicitThrow)
}


object ExplicitThrow : InstructionExecution() {
    override fun treeFormat(buffer: StringBuilder, prefix: String, childPrefix: String) {
        buffer.append(prefix + "explicit throw" + "\n")
    }
}

fun FunctionCall.ret() {
    addInstruction(Return)
}


object Return : InstructionExecution() {
    override fun treeFormat(buffer: StringBuilder, prefix: String, childPrefix: String) {
        buffer.append(prefix + "return" + "\n")
    }
}

fun FunctionCall.pass() {
    addInstruction(Pass)
}

fun function(signature: String, block: FunctionCall.() -> Unit): FunctionCall = FunctionCall(signature).apply(block)

fun FunctionCall.invoke(name: String, block: FunctionCall.() -> Unit) {
    addInstruction(FunctionCall(name).apply(block))
}