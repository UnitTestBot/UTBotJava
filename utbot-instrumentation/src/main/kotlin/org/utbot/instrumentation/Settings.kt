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

    // TODO: maybe add this guide to confluence?
    /**
     * If true, runs the child process with the ability to attach a debugger.
     *
     * To debug the child process, set the breakpoint in the childProcessRunner.start() line
     * and in the child process's main function and run the main process.
     * Then run the remote JVM debug configuration in IDEA.
     * If you see the message in console about successful connection, then
     * the debugger is attached successfully.
     * Now you can put the breakpoints in the child process and debug
     * both processes simultaneously.
     *
     * @see [org.utbot.instrumentation.process.ChildProcessRunner.cmds]
     */
    const val runChildProcessWithDebug = false

    var defaultConcreteExecutorPoolSize = 10
}