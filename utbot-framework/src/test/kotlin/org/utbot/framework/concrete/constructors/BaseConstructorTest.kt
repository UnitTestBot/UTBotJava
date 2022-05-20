package org.utbot.framework.concrete.constructors

import org.utbot.engine.ValueConstructor
import org.utbot.framework.concrete.UtModelConstructor
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.id
import java.util.IdentityHashMap
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach

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
        val model = UtModelConstructor(IdentityHashMap()).construct(value, value::class.java.id)

        Assertions.assertTrue(model is UtAssembleModel)

        @Suppress("UNCHECKED_CAST")
        return ValueConstructor().construct(listOf(model)).single().value as T
    }
}