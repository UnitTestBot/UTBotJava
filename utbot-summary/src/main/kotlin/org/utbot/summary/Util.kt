package org.utbot.summary

import org.utbot.framework.plugin.api.Step
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.summary.tag.BasicTypeTag
import org.utbot.summary.tag.getBasicTypeTag
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod
import soot.SootClass
import soot.SootMethod
import soot.jimple.JimpleBody
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInvokeStmt


fun <T> List<T>.isEqualPath(other: List<T>): Boolean {

    if (this.size != other.size) {
        return false
    }

    return this.zip(other).all { (x, y) -> x == y }
}

fun List<Step>.invokes() = this.filter { getBasicTypeTag(it.stmt) == BasicTypeTag.Invoke }.map { it.stmt }


fun List<Step>.invokeJimpleMethods(): List<SootMethod> =
    this.invokes().filter { it is JInvokeStmt || it is JAssignStmt }
        .map {
            it.invokeExpr.method
        }

fun stepsUpToDepth(executions: List<UtSymbolicExecution>, depth: Int) {
    for (execution in executions) {
        execution.path.clear()
        execution.path.addAll(execution.fullPath.filter { it.depth <= depth })
    }
}

/*
* from 0 to 100
* */
fun percentageDiverseExecutions(executions: List<UtSymbolicExecution>): Int {
    if (executions.isEmpty()) return 100
    val diverseExecutions = numberDiverseExecutionsBasedOnPaths(executions)
    return 100 * diverseExecutions.size / executions.size
}

fun numberDiverseExecutionsBasedOnPaths(executions: List<UtSymbolicExecution>) = executions.filter { current ->
    executions.filter { it != current }.any { other ->
        current.path.isEqualPath(other.path)
    }.not()
}

/*
 * Copy from framework -> Extensions.kt
 */
fun SootClass.adjustLevel(level: Int) {
    if (resolvingLevel() < level) {
        setResolvingLevel(level)
    }
}

fun SootMethod.jimpleBody(): JimpleBody {
    declaringClass.adjustLevel(SootClass.BODIES)
    return retrieveActiveBody() as JimpleBody
}

val <R> UtMethod<R>.javaConstructor: Constructor<*>?
    get() = (callable as? KFunction<*>)?.javaConstructor

val <R> UtMethod<R>.javaMethod: Method?
    get() = (callable as? KFunction<*>)?.javaMethod ?: (callable as? KProperty<*>)?.getter?.javaMethod

val <R> UtMethod<R>.displayName: String
    get() {
        val methodName = this.callable.name
        val javaMethod = this.javaMethod ?: this.javaConstructor
        if (javaMethod != null) {
            val parameters = javaMethod.parameters.joinToString(separator = ", ") { it.type.canonicalName }
            return "${methodName}($parameters)"
        }
        return "${methodName}()"
    }