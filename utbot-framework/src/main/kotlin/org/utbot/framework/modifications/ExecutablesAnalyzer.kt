package org.utbot.framework.modifications

import org.utbot.engine.fieldId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.id
import org.utbot.framework.util.executableId
import soot.Scene
import soot.SootMethod
import soot.jimple.InvokeExpr
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInstanceFieldRef
import soot.jimple.internal.JInvokeStmt

/**
 * Analyzer of class executables based on Soot.
 */
class ExecutablesAnalyzer {
    /**
     * Cache to map methods with their Soot methods.
     */
    private val executablesCache: MutableMap<ExecutableId, SootMethod> = mutableMapOf()

    private val javaClassNamesPrefix = "java"
    private val anyClassFunctionsNames = setOf("equals", "hashCode", "toString")

    /**
     * Collects all root executables in classes.
     */
    fun collectRootExecutables(classIds: Set<ClassId>): Set<ExecutableId> {
        executablesCache.entries.removeIf { it.value.declaringClass.id in classIds }

        val sootMethods = findSootMethodsUnderAnalysis(classIds)

        val executableIds = mutableSetOf<ExecutableId>()
        sootMethods
            .forEach { sootMethod ->
                val executableId = sootMethod.executableId
                executableIds.add(executableId)
                executablesCache[executableId] = sootMethod
            }

        return executableIds
    }

    /**
     * Finds declaring class of the method.
     */
    fun findDeclaringClass(executableId: ExecutableId): ClassId {
        val sootMethod = executablesCache[executableId] ?: error("No method ${executableId.name} in cache")
        return sootMethod.declaringClass.id
    }

    /**
     * Finds fields modified in Jimple code of this method.
     */
    fun findModificationsInJimple(executableId: ExecutableId): Set<FieldId> {
        val sootMethod = executablesCache[executableId] ?: error("No method ${executableId.name} in soot cache")

        val jimpleBody = retrieveJimpleBody(sootMethod) ?: return emptySet()
        return jimpleBody.units
            .filterIsInstance<JAssignStmt>()
            .mapNotNull { it.leftOp as? JInstanceFieldRef }
            .map { it.field.fieldId }
            .toSet()
    }

    /**
     * Finds methods invoked in Jimple code of this method.
     * Method can be invoked in JInvokeStmt or in right part of JAssignStmt.
     */
    fun findInvocationsInJimple(executableId: ExecutableId): Set<ExecutableId> {
        val sootMethod = executablesCache[executableId] ?: error("No method ${executableId.name} in soot cache")

        val jimpleBody = retrieveJimpleBody(sootMethod) ?: return emptySet()
        val sootMethods = jimpleBody.units
            .mapNotNull { stmt ->
                when (stmt) {
                    is JInvokeStmt -> stmt.invokeExpr
                    is JAssignStmt -> stmt.rightOp as? InvokeExpr
                    else -> null
                }
            }
            .map { it.method }
            .filterNot { it.declaringClass.name.startsWith(javaClassNamesPrefix) }
            .filterNot { it == sootMethod }
            .toSet()

        sootMethods.forEach {
            val methodId = it.executableId
            executablesCache.getOrPut(methodId) { it }
        }
        return sootMethods.map { it.executableId }.toSet()
    }

    /**
     * Finds all matching [SootMethod] in requested classes and their hierarchy.
     */
    private fun findSootMethodsUnderAnalysis(classIds: Set<ClassId>): Set<SootMethod> {
        val hierarchy = Scene.v().activeHierarchy
        val sootClasses = classIds
            .map { Scene.v().getSootClass(it.name) }
            .filterNot { it.isInterface }

        val sootMethods = mutableSetOf<SootMethod>()
        sootClasses.forEach { sootClass ->
            val methods = sootClass.methods.filter { isUnderAnalysis(it) }
            val methodDeclarations = methods.map { it.declaration }
            sootMethods.addAll(methods)

            val classHierarchy = hierarchy.getSuperclassesOf(sootClass)
            val classMethods = classHierarchy
                .flatMap { it.methods }
                //filter override methods implementation in base classes
                .filter { isUnderAnalysis(it) &&  it.declaration !in methodDeclarations }
            sootMethods.addAll(classMethods)
        }

        return sootMethods
    }

    /**
     * Verifies that a method can be considered as a modificator.
     */
    private fun isUnderAnalysis(method: SootMethod): Boolean =
        !method.isPrivate
                && method.name !in anyClassFunctionsNames
                && !method.declaringClass.name.startsWith(javaClassNamesPrefix)
}

