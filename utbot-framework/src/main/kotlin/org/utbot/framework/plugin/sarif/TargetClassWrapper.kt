package org.utbot.framework.plugin.sarif

import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.util.executableId
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
    val sarifReportFile: File,
    val testPrivateMethods: Boolean = false
) {
    /**
     * Returns the methods of the class [classUnderTest] declared by the user.
     */
    val targetMethods: List<UtMethod<*>> = run {
        val allDeclaredMethods = classUnderTest.java.declaredMethods
        val neededDeclaredMethods = if (testPrivateMethods) {
            allDeclaredMethods.toList()
        } else {
            allDeclaredMethods.filter {
                !it.executableId.isPrivate
            }
        }
        neededDeclaredMethods.map {
            UtMethod(it.kotlinFunction as KCallable<*>, classUnderTest)
        }
    }
}