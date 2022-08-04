package org.utbot.framework.codegen.model

import org.utbot.framework.plugin.api.*
import org.utbot.python.PythonTestSet

object PythonCodeGenerator {
    fun generateAsString(
        classUnderTest: ClassId,
        testSets: Collection<PythonTestSet>
    ): String {
//        val codegen = CodeGenerator(classUnderTest)
//        return codegen.generateAsStringWithTestReport(
//            testSets.map {
//                CgMethodTestSet(
//                    MethodId(
//                        classUnderTest,
//                        it.method.name,
//                        ClassId(it.method.returnAnnotation ?: pythonNoneClassId.name),
//                        it.method.arguments.map { arg -> ClassId(arg.name) }
//                    )
//                )
//            }
//        ).generatedCode
        return ""
    }
}