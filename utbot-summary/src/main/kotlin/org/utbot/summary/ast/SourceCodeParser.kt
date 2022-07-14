package org.utbot.summary.ast

import com.github.javaparser.JavaParser
import com.github.javaparser.ParseResult
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import org.utbot.framework.plugin.api.UtMethodTestSet
import java.io.File
import kotlin.math.abs
import soot.SootMethod

const val OUTER_INNER_CLASSES_DELIMITER = '$'

class SourceCodeParser {
    /**
     * source: String - can be path to source code as string or source code as string
     */
    private val cu: ParseResult<CompilationUnit>
    var methodAST: MethodDeclaration? = null

    constructor(sourceFile: File, testSet: UtMethodTestSet) {
        val parser = JavaParser()
        cu = parser.parse(sourceFile)
        val className = testSet.method.clazz.simpleName
        val methodName = testSet.method.callable.name

        val lineNumbers = testSet.jimpleBody?.units?.map { it.javaSourceStartLineNumber }
        val maxLineNumber = lineNumbers?.maxOrNull()
        if (className != null && maxLineNumber != null) findMethod(className, methodName, maxLineNumber)
    }

    constructor(sootMethod: SootMethod, sourceFile: File) {
        val methodName = sootMethod.name
        val className = sootMethod.declaredClassName


        val maxLineNumber =
            if (sootMethod.hasActiveBody())
                sootMethod.retrieveActiveBody()?.units?.maxOfOrNull { it.javaSourceStartLineNumber }
            else null
        val parser = JavaParser()
        cu = parser.parse(sourceFile)
        if (maxLineNumber != null) findMethod(className, methodName, maxLineNumber)
    }

    /**
     * Finds method by class name, method name and last code line in jimple body
     *
     * @return MethodDeclaration of found method or null if method was not found
     */
    private fun findMethod(className: String, methodName: String, maxJimpleLine: Int): MethodDeclaration? {
        cu.ifSuccessful {
            val clazz = it.types.firstOrNull { clazz ->
                clazz.name.identifier == className
            } ?: traverseInnerClassDeclarations(
                it.types.flatMap { declaration -> declaration.childNodes },
                className
            )

            if (clazz != null) {
                val allMethods = clazz.methods.filter { method ->
                    method.name.identifier == methodName
                }.toTypedArray()

                methodAST = when {
                    allMethods.size == 1 -> allMethods.first()
                    allMethods.isNotEmpty() -> {
                        val lineDiffs = allMethods
                            .map { method -> method.end.get().line }
                            .map { endLine -> abs(endLine - maxJimpleLine) }
                        allMethods[lineDiffs.indexOf(lineDiffs.minOrNull())]
                    }
                    else -> null
                }
            }
        }
        return methodAST
    }

    /**
     * Sets identifier to each node
     * Identifier is ClassOrInterfaceDeclaration
     */
    private fun traverseInnerClassDeclarations(
        nodes: List<Node>, className: String
    ): TypeDeclaration<*>? = nodes.filterIsInstance<ClassOrInterfaceDeclaration>()
        .firstOrNull { it.name.identifier == className }
}

/**
 * Returns name of the class that SootMethod is in
 */
val SootMethod.declaredClassName: String
    get() = if (this.declaringClass.isInnerClass) {
        // an inner class name declaration follows pattern: OuterClass_Delimiter_InnerClass
        this.declaringClass.javaStyleName.substringAfterLast(OUTER_INNER_CLASSES_DELIMITER)
    } else {
        this.declaringClass.javaStyleName
    }