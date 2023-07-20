package org.utbot.examples.et

import org.utbot.examples.objects.ObjectWithStaticFieldsClass
import org.utbot.examples.objects.ObjectWithStaticFieldsExample
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.jField
import org.utbot.instrumentation.execute
import org.utbot.instrumentation.instrumentation.et.ExecutionTraceInstrumentation
import org.utbot.instrumentation.withInstrumentation
import kotlin.reflect.jvm.javaField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StaticsUsageDetectionTest {
    lateinit var utContext: AutoCloseable

    @BeforeEach
    fun initContext() {
        utContext = UtContext.setUtContext(UtContext(ClassLoader.getSystemClassLoader()))
    }

    @AfterEach
    fun closeConctext() {
        utContext.close()
    }


    @Test
    fun testStaticsUsageOneUsage() {
        withInstrumentation(
            ExecutionTraceInstrumentation.Factory,
            ObjectWithStaticFieldsExample::class.java.protectionDomain.codeSource.location.path
        ) {
            val instance = ObjectWithStaticFieldsExample()
            val classInstance = ObjectWithStaticFieldsClass()
            classInstance.x = 200
            classInstance.y = 200
            val result = it.execute(ObjectWithStaticFieldsExample::setStaticField, arrayOf(instance, classInstance))
            assertEquals(ObjectWithStaticFieldsClass::staticValue.javaField, result.usedStatics.single().jField)
        }
    }

    @Test
    fun testStaticsUsageZeroUsages() {
        withInstrumentation(
            ExecutionTraceInstrumentation.Factory,
            ObjectWithStaticFieldsExample::class.java.protectionDomain.codeSource.location.path
        ) {
            val instance = ObjectWithStaticFieldsExample()
            val classInstance = ObjectWithStaticFieldsClass()
            val result = it.execute(ObjectWithStaticFieldsExample::setStaticField, arrayOf(instance, classInstance))
            assertTrue(result.usedStatics.isEmpty())
        }
    }
}