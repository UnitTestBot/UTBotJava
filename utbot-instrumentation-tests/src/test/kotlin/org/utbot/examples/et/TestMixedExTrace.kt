package org.utbot.examples.et

import org.utbot.examples.samples.et.ClassMixedWithNotInstrumented_Instr
import org.utbot.examples.samples.et.ClassMixedWithNotInstrumented_Not_Instr
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.et.ExecutionTraceInstrumentation
import org.utbot.instrumentation.instrumentation.et.convert
import org.utbot.instrumentation.instrumentation.et.function
import org.utbot.instrumentation.instrumentation.et.invoke
import org.utbot.instrumentation.instrumentation.et.pass
import org.utbot.instrumentation.instrumentation.et.ret
import org.utbot.instrumentation.util.Isolated
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class TestMixedExTrace {
    lateinit var utContext: AutoCloseable


    val CLASSPATH = ClassMixedWithNotInstrumented_Instr::class.java.protectionDomain.codeSource.location.path

    @Disabled("The execution trace of mixed calls is not properly supported yet")
    // `mixed calls` means such calls: A -> {B -> {A -> ...}, ... }, where A has been instrumented but B has not.
    @Test
    fun testMixedDoesNotThrow() {
        ConcreteExecutor(
            ExecutionTraceInstrumentation.Factory,
            CLASSPATH
        ).use {
            val A = Isolated(ClassMixedWithNotInstrumented_Instr::a, it)
            val B = Isolated(ClassMixedWithNotInstrumented_Not_Instr::b, it)

            val res = A(1)
            assertEquals(
                function(A.signature) {
                    pass()
                    invoke(B.signature) { // TODO: think on clear DSL API for not instrumented calls
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                    }
                    ret()
                },
                convert(res)
            )
        }
    }
}