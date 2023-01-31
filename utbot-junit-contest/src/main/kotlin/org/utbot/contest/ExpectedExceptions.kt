package org.utbot.contest

import java.io.File

class ExpectedExceptionsForMethod(
    val exceptionNames: List<String>
)

class ExpectedExceptionsForClass {

    private val byMethodName: MutableMap<String, ExpectedExceptionsForMethod> =
        mutableMapOf()

    fun getForMethod(methodName: String): ExpectedExceptionsForMethod =
        byMethodName[methodName] ?: ExpectedExceptionsForMethod(listOf())

    fun setForMethod(methodName: String, exceptionNames: List<String>) {
        byMethodName[methodName] = ExpectedExceptionsForMethod(exceptionNames)
    }
}

class ExpectedExceptionsForProject {

    private val byClassName: MutableMap<String, ExpectedExceptionsForClass> =
        mutableMapOf()

    fun getForClass(className: String): ExpectedExceptionsForClass =
        byClassName[className] ?: ExpectedExceptionsForClass()

    fun setForClass(className: String, methodName: String, exceptionNames: List<String>) {
        val forClass = byClassName.getOrPut(className) { ExpectedExceptionsForClass() }
        forClass.setForMethod(methodName, exceptionNames)
    }
}

fun parseExceptionsFile(exceptionsDescriptionFile: File): ExpectedExceptionsForProject {
    if (!exceptionsDescriptionFile.exists()) {
        return ExpectedExceptionsForProject()
    }

    val forProject = ExpectedExceptionsForProject()
    for (methodDescription in exceptionsDescriptionFile.readLines()) {
        val methodFqn = methodDescription.substringBefore(':').trim()
        val classFqn = methodFqn.substringBeforeLast('.')
        val methodName = methodFqn.substringAfterLast('.')
        val exceptionFqns = methodDescription.substringAfter(':')
            .split(' ')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        forProject.setForClass(classFqn, methodName, exceptionFqns)
    }

    return forProject
}
