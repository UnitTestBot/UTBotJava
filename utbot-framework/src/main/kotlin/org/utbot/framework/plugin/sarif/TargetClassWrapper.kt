package org.utbot.framework.plugin.sarif

import org.utbot.framework.plugin.api.UtMethod
import java.io.File
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction

/**
 * Contains information about the class for which we are creating a SARIF report.
 */
data class TargetClassWrapper(
    val qualifiedName: String,
    val classUnderTest: KClass<*>,
    val sourceCodeFile: File,
    val testsCodeFile: File,
    val sarifReportFile: File
) {
    /**
     * Returns the methods of the class [classUnderTest] declared by the user.
     */
    fun targetMethods() =
        classUnderTest.java.declaredMethods.map {
            UtMethod(it.kotlinFunction as KCallable<*>, classUnderTest)
        }
}