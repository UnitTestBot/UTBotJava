package org.utbot.examples.et

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.utbot.examples.samples.et.ClassSimple
import org.utbot.examples.samples.et.ClassSimpleCatch
import org.utbot.instrumentation.instrumentation.et.*
import org.utbot.instrumentation.util.Isolated
import org.utbot.instrumentation.withInstrumentation

class TestExecutionBranchTraceInstrumentation {
    lateinit var utContext: AutoCloseable

    val CLASSPATH = ClassSimple::class.java.protectionDomain.codeSource.location.path

    @Test
    fun testClassSimple() {
        withInstrumentation(
            ExecutionBranchTraceInstrumentation.Factory,
            CLASSPATH
        ) { executor ->
            val doesNotThrow = Isolated(ClassSimple::doesNotThrow, executor)
            val alwaysThrows = Isolated(ClassSimple::alwaysThrows, executor)
            val maybeThrows = Isolated(ClassSimple::maybeThrows, executor)

            val et1 = doesNotThrow()
            Assertions.assertEquals(
                function(doesNotThrow.signature) {
                    pass()
                    ret()
                },
                convert(et1)
            )

            val et2 = alwaysThrows()
            Assertions.assertEquals(
                function(alwaysThrows.signature) {
                    pass()
                    explThr()
                },
                convert(et2)
            )

            val et3 = maybeThrows(-1)
            Assertions.assertEquals(
                function(maybeThrows.signature) {
                    pass()
                    explThr()
                },
                convert(et3)
            )

            val et4 = maybeThrows(0)
            Assertions.assertEquals(
                function(maybeThrows.signature) {
                    pass()
                    ret()
                },
                convert(et4)
            )
        }
    }

    @Test
    fun testClasSimpleCatch() {
        withInstrumentation(
            ExecutionBranchTraceInstrumentation.Factory,
            CLASSPATH
        ) { executor ->
            val A = Isolated(ClassSimpleCatch::A, executor)
            val A_catches = Isolated(ClassSimpleCatch::A_catches, executor)
            val A_catchesWrongException = Isolated(ClassSimpleCatch::A_catchesWrongException, executor)
            val A_doesNotCatch = Isolated(ClassSimpleCatch::A_doesNotCatch, executor)

            val B = Isolated(ClassSimpleCatch::B, executor)
            val B_throws = Isolated(ClassSimpleCatch::B_throws, executor)

            val et1 = A()
            Assertions.assertEquals(
                function(A.signature) {
                    pass()
                    invoke(B.signature) {
                        pass()
                        ret()
                    }
                    ret()
                },
                convert(et1)
            )

            val et2 = A_catches()
            Assertions.assertEquals(
                function(A_catches.signature) {
                    pass()
                    invoke(B_throws.signature) {
                        implThr()
                    }
                    pass()
                    ret()
                },
                convert(et2)
            )

            val et3 = A_catchesWrongException()
            Assertions.assertEquals(
                function(A_catchesWrongException.signature) {
                    pass()
                    invoke(B_throws.signature) {
                        implThr()
                    }
                },
                convert(et3)
            )

            val et4 = A_doesNotCatch()
            Assertions.assertEquals(
                function(A_doesNotCatch.signature) {
                    pass()
                    invoke(B_throws.signature) {
                        implThr()
                    }
                },
                convert(et4)
            )
        }
    }
}
