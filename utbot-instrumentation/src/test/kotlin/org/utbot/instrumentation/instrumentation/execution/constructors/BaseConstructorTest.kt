package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.framework.plugin.api.util.constructor.ValueConstructor
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.id
import java.util.IdentityHashMap
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.jClass

abstract class BaseConstructorTest {
    private lateinit var cookie: AutoCloseable

    @BeforeEach
    fun setup() {
        cookie = UtContext.setUtContext(UtContext(ClassLoader.getSystemClassLoader()))
    }

    @AfterEach
    fun tearDown() {
        cookie.close()
    }

    protected fun <T : Any> computeReconstructed(value: T): T {
        val model = UtModelConstructor(
            objectToModelCache = IdentityHashMap(),
            utCustomModelConstructorFinder = ::findUtCustomModelConstructor
        ).construct(value, value::class.java.id)

        Assertions.assertTrue(model is UtAssembleModel)

        @Suppress("UNCHECKED_CAST")
        return ValueConstructor().construct(listOf(model)).single().value as T
    }

    protected open fun findUtCustomModelConstructor(classId: ClassId): UtCustomModelConstructor? =
        javaStdLibCustomModelConstructors[classId.jClass]?.invoke()
}