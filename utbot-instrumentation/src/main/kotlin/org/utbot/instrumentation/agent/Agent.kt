package org.utbot.instrumentation.agent

import java.lang.instrument.Instrumentation

/**
 * Agent class.
 *
 * It should be compiled into separate jar file (agent.jar) and passed as an agent option.
 */

class Agent {
    companion object {
        /**
         * Transformer which allows to transform only certain classes.
         * These classes will be passed with the help [org.utbot.instrumentation.ConcreteExecutor.loadClass] function.
         */
        val dynamicClassTransformer = DynamicClassTransformer()

        /**
         * It will be run before [org.utbot.instrumentation.process.main] function according to javaagent specification.
         *
         * Allows to transform classes bytecode during loading into JVM.
         */
        @JvmStatic
        fun premain(arguments: String?, instrumentation: Instrumentation) {
            instrumentation.addTransformer(dynamicClassTransformer)
        }
    }
}