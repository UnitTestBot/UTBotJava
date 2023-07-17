package org.utbot.examples.et

import org.utbot.examples.samples.et.ClassBinaryRecursionWithThrow
import org.utbot.examples.samples.et.ClassBinaryRecursionWithTrickyThrow
import org.utbot.examples.samples.et.ClassSimple
import org.utbot.examples.samples.et.ClassSimpleCatch
import org.utbot.examples.samples.et.ClassSimpleNPE
import org.utbot.examples.samples.et.ClassSimpleRecursive
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.et.ExecutionTraceInstrumentation
import org.utbot.instrumentation.instrumentation.et.convert
import org.utbot.instrumentation.instrumentation.et.explThr
import org.utbot.instrumentation.instrumentation.et.function
import org.utbot.instrumentation.instrumentation.et.implThr
import org.utbot.instrumentation.instrumentation.et.invoke
import org.utbot.instrumentation.instrumentation.et.pass
import org.utbot.instrumentation.instrumentation.et.ret
import org.utbot.instrumentation.util.Isolated
import kotlin.reflect.full.declaredFunctions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class TestSimpleExTrace {
    lateinit var utContext: AutoCloseable

    val CLASSPATH = ClassSimple::class.java.protectionDomain.codeSource.location.path

    /**
     * #1. doesNotThrow
     * #2. alwaysThrows
     * #3. maybeThrows(-1) maybeThrows(0)
     */
    @Test
    fun testClassSimple() {
        ConcreteExecutor(
            ExecutionTraceInstrumentation.Factory(),
            CLASSPATH
        ).use {
            val alwaysThrows = Isolated(ClassSimple::alwaysThrows, it)
            val maybeThrows = Isolated(ClassSimple::maybeThrows, it)
            val doesNotThrow = Isolated(ClassSimple::doesNotThrow, it)

            val et1 = doesNotThrow()
            assertEquals(
                function(doesNotThrow.signature) {
                    pass()
                    ret()
                },
                convert(et1)
            )

            val et2 = alwaysThrows()
            assertEquals(
                function(alwaysThrows.signature) {
                    pass()
                    explThr()
                },
                convert(et2)
            )

            val et3 = maybeThrows(-1)
            assertEquals(
                function(maybeThrows.signature) {
                    pass()
                    explThr()
                },
                convert(et3)
            )

            val et4 = maybeThrows(0)
            assertEquals(
                function(maybeThrows.signature) {
                    pass()
                    ret()
                },
                convert(et4)
            )
        }
    }


    /**
     * #1. A
     * #2. A_catches
     * #3. A_doesNotCatch
     * #4. A_catchesWrongException
     */
    @Test
    fun testClasSimpleCatch() {
        ConcreteExecutor(
            ExecutionTraceInstrumentation.Factory(),
            CLASSPATH
        ).use {
            val A = Isolated(ClassSimpleCatch::A, it)
            val A_catches = Isolated(ClassSimpleCatch::A_catches, it)
            val A_catchesWrongException = Isolated(ClassSimpleCatch::A_catchesWrongException, it)
            val A_doesNotCatch = Isolated(ClassSimpleCatch::A_doesNotCatch, it)

            val B = Isolated(ClassSimpleCatch::B, it)
            val B_throws = Isolated(ClassSimpleCatch::B_throws, it)


            val et1 = A()
            assertEquals(
                function(A.signature) {
                    invoke(B.signature) {
                        pass()
                        ret()
                    }
                    pass()
                    ret()
                },
                convert(et1)
            )

            val et2 = A_catches()
            assertEquals(
                function(A_catches.signature) {
                    invoke(B_throws.signature) {
                        pass()
                        implThr()
                    }
                    pass()
                    ret()
                },
                convert(et2)
            )

            val et3 = A_catchesWrongException()
            assertEquals(
                function(A_catchesWrongException.signature) {
                    invoke(B_throws.signature) {
                        pass()
                        implThr()
                    }
                },
                convert(et3)
            )

            val et4 = A_doesNotCatch()
            assertEquals(
                function(A_doesNotCatch.signature) {
                    invoke(B_throws.signature) {
                        pass()
                        implThr()
                    }
                },
                convert(et4)
            )
        }
    }

    /**
     * #1. A(3)
     * #2. A_recursive(3, 2)
     * #3. A_recursive(3, 1)
     */
    @Test
    fun testClassSimpleRecursive() {
        ConcreteExecutor(
            ExecutionTraceInstrumentation.Factory(),
            CLASSPATH
        ).use {
            val A = Isolated(ClassSimpleRecursive::A, it)
            val A_recursive = Isolated(ClassSimpleRecursive::A_recursive, it)

            val et1 = A(3)
            assertEquals(
                function(A.signature) {
                    pass()
                    invoke(A.signature) {
                        pass()
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                        ret()
                    }
                    ret()
                },
                convert(et1)
            )

            val et2 = A_recursive(3, 2)
            assertEquals(
                function(A_recursive.signature) {
                    pass()
                    invoke(A_recursive.signature) {
                        pass()
                        invoke(A_recursive.signature) {
                            pass()
                            explThr()
                        }
                        pass()
                        explThr()
                    }
                    pass()
                    ret()
                },
                convert(et2)
            )

            val et3 = A_recursive(3, 1)
            assertEquals(
                function(A_recursive.signature) {
                    pass()
                    invoke(A_recursive.signature) {
                        pass()
                        invoke(A_recursive.signature) {
                            pass()
                            explThr()
                        }
                        pass()
                        explThr()
                    }
                },
                convert(et3)
            )
        }
    }

    /**
     * #1. A(1, 10, 2)
     * #2. A_catchesAll(true, 2)
     * #3. A_notAll(right, 2)
     */
    @Test
    fun testClassBinaryRecursionWithTrickyThrow() {
        ConcreteExecutor(
            ExecutionTraceInstrumentation.Factory(),
            CLASSPATH
        ).use {
            val A = Isolated(ClassBinaryRecursionWithTrickyThrow::A, it)
            val A_catchesAll = Isolated(ClassBinaryRecursionWithTrickyThrow::A_catchesAll, it)
            val A_notAll = Isolated(ClassBinaryRecursionWithTrickyThrow::A_notAll, it)

            val et1 = A(1, 10, 2)
            assertEquals(
                function(A.signature) {
                    pass()
                    invoke(A.signature) {
                        pass()
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        ret()
                    }
                    pass()
                    invoke(A.signature) {
                        pass()
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        ret()
                    }
                    pass()
                    ret()
                },
                convert(et1)
            )

            val et2 = A_catchesAll(true, 2)

            assertEquals(
                function(A_catchesAll.signature) {
                    pass()
                    invoke(A_catchesAll.signature) {
                        pass()
                        invoke(A_catchesAll.signature) {
                            pass()
                            explThr()
                        }
                        pass()
                        invoke(A_catchesAll.signature) {
                            pass()
                            explThr()
                        }
                        pass()
                        ret()
                    }
                    pass()
                    invoke(A_catchesAll.signature) {
                        pass()
                        invoke(A_catchesAll.signature) {
                            pass()
                            explThr()
                        }
                    }
                    pass()
                    ret()
                },
                convert(et2)
            )

            val et3 = A_notAll(false, 2)
            assertEquals(
                function(A_notAll.signature) {
                    pass()
                    invoke(A_notAll.signature) {
                        pass()
                        invoke(A_notAll.signature) {
                            pass()
                            explThr()
                        }
                    }
                    pass()
                    invoke(A_notAll.signature) {
                        pass()
                        invoke(A_notAll.signature) {
                            pass()
                            explThr()
                        }
                        pass()
                        invoke(A_notAll.signature) {
                            pass()
                            explThr()
                        }
                    }
                    pass()
                    ret()
                },
                convert(et3)
            )
        }
    }


    /**
     * #1. A(1, 2, false)
     * #2. A(1, 2, true)
     */
    @Test
    fun testClassBinaryRecursionWithThrow() {
        ConcreteExecutor(
            ExecutionTraceInstrumentation.Factory(),
            CLASSPATH
        ).use {
            val A = Isolated(ClassBinaryRecursionWithThrow::A, it)
            val B = Isolated(ClassBinaryRecursionWithThrow::class.declaredFunctions.first { it.name == "B" }, it)

            val et1 = A(1, 2, false)
            assertEquals(
                function(A.signature) {
                    pass()
                    invoke(B.signature) {
                        pass()
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        invoke(B.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        ret()
                    }
                    pass()
                    invoke(A.signature) {
                        pass()
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                        ret()
                    }
                    ret()
                },
                convert(et1)
            )

            val et2 = A(1, 2, true)
            assertEquals(
                function(A.signature) {
                    pass()
                    invoke(B.signature) {
                        pass()
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        invoke(B.signature) {
                            pass()
                            explThr()
                        }
                        pass()
                        ret()
                    }
                    pass()
                    invoke(A.signature) {
                        pass()
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        invoke(A.signature) {
                            pass()
                            ret()
                        }
                        ret()
                    }
                    ret()
                },
                convert(et2)
            )
        }
    }

    /**
     * #1. A(false)
     * #2. A(true)
     */
    @Test
    fun testClassSimpleNPE() {
        ConcreteExecutor(
            ExecutionTraceInstrumentation.Factory(),
            CLASSPATH
        ).use {
            val A = Isolated(ClassSimpleNPE::A, it)
            val B = Isolated(ClassSimpleNPE::B, it)
            val C = Isolated(ClassSimpleNPE::C, it)
            val D = Isolated(ClassSimpleNPE::D, it)

            val thisObject = ClassSimpleNPE()

            val et1 = A(thisObject, false)
            assertEquals(
                function(A.signature) {
                    pass()
                    invoke(B.signature) {
                        pass()
                        invoke(B.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        ret()
                    }
                    pass()
                    invoke(C.signature) {
                        pass()
                        invoke(C.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        ret()
                    }
                    pass()
                    invoke(D.signature) {
                        pass()
                        invoke(D.signature) {
                            pass()
                            invoke(D.signature) {
                                pass()
                                ret()
                            }
                            pass()
                            ret()
                        }
                        pass()
                        ret()
                    }
                    ret()
                },
                convert(et1)
            )

            val et2 = A(thisObject, true)
            assertEquals(
                function(A.signature) {
                    pass()
                    invoke(B.signature) {
                        pass()
                        invoke(B.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        ret()
                    }
                    pass()
                    invoke(C.signature) {
                        pass()
                        invoke(C.signature) {
                            pass()
                            ret()
                        }
                        pass()
                        ret()
                    }
                    pass()
                    invoke(D.signature) {
                        pass()
                        implThr()
                    }
                },
                convert(et2)
            )
        }
    }
}