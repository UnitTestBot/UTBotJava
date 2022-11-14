package org.utbot.instrumentation

import org.objectweb.asm.Opcodes

object Settings {
    const val ASM_API = Opcodes.ASM7

    /**
     * Constants used in bytecode instrumentation.
     */
    const val PROBES_ARRAY_NAME = "\$__instrs__"

    const val PROBES_ARRAY_DESC = "[Z"

    const val TRACE_ARRAY_SIZE: Int = 1 shl 20

    var defaultConcreteExecutorPoolSize = 10
}