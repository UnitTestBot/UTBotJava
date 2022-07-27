package org.utbot.instrumentation.instrumentation.et

import org.utbot.jcdb.api.FieldId

// TODO: refactor this later

// TODO: document this

sealed class Node {
    abstract fun treeFormat(buffer: StringBuilder, prefix: String, childPrefix: String)
}

// TODO: document this
data class EtInstruction(
    val className: String,
    val methodSignature: String,
    val callId: Int,
    val id: Long,
    val line: Int,
    val instructionData: InstructionData
) : Node() {
    override fun toString(): String {
        return "$methodSignature : $id :: $line" + if (instructionData !is CommonInstruction) " $instructionData" else ""
    }

    override fun treeFormat(buffer: StringBuilder, prefix: String, childPrefix: String) {
        buffer.append("$prefix${toString()}\n")
    }
}

// TODO: document this

data class TraceNode(
    val className: String, // TODO: ClassId
    val methodSignature: String,
    val callId: Int,
    val depth: Int,
    val instructions: MutableList<Node>
) : Node() {
    override fun treeFormat(buffer: StringBuilder, prefix: String, childPrefix: String) {
        buffer.append(prefix + "$className::$methodSignature-$callId\n")
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
        val buffer = StringBuilder(50)
        treeFormat(buffer, "", "")
        return buffer.toString()
    }
}

// TODO: document this

data class Trace(
    val root: TraceNode,
    val usedStatics: List<FieldId>
) {
    override fun toString(): String {
        return "Trace:\n$root"
    }
}
