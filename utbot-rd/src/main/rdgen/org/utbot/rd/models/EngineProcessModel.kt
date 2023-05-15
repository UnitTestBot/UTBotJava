@file:Suppress("unused")
package org.utbot.rd.models

import com.jetbrains.rd.generator.nova.*

object EngineProcessRoot : Root()

object RdInstrumenterAdapter: Ext(EngineProcessRoot) {
    val computeSourceFileByClassArguments = structdef {
        field("canonicalClassName", PredefinedType.string)
    }
    init {
        call("computeSourceFileByClass", computeSourceFileByClassArguments, PredefinedType.string.nullable).async
    }
}

object RdSourceFindingStrategy : Ext(EngineProcessRoot) {
    val sourceStrategyMethodArgs = structdef {
        field("testSetId", PredefinedType.long)
        field("classFqn", PredefinedType.string)
        field("extension", PredefinedType.string.nullable)
    }

    init {
        call("testsRelativePath", PredefinedType.long, PredefinedType.string).async
        call("getSourceRelativePath", sourceStrategyMethodArgs, PredefinedType.string).async
        call("getSourceFile", sourceStrategyMethodArgs, PredefinedType.string.nullable).async
    }
}

object EngineProcessModel : Ext(EngineProcessRoot) {
    val jdkInfo = structdef {
        field("path", PredefinedType.string)
        field("version", PredefinedType.int)
    }

    val testGeneratorParams = structdef {
        field("buildDir", array(PredefinedType.string))
        field("classpath", PredefinedType.string.nullable)
        field("dependencyPaths", PredefinedType.string)
        field("jdkInfo", jdkInfo)
        field("applicationContext", array(PredefinedType.byte))
    }
    val generateParams = structdef {
        // generate
        field("methods", array(PredefinedType.byte))
        field("mockStrategy", PredefinedType.string)
        field("chosenClassesToMockAlways", array(PredefinedType.byte))
        field("timeout", PredefinedType.long)
        // testflow
        field("generationTimeout", PredefinedType.long)
        field("isSymbolicEngineEnabled", PredefinedType.bool)
        field("isFuzzingEnabled", PredefinedType.bool)
        field("fuzzingValue", PredefinedType.double)
        // method filters
        field("searchDirectory", PredefinedType.string)
    }
    val generateResult = structdef {
        field("notEmptyCases", PredefinedType.int)
        field("testSetsId", PredefinedType.long)
    }
    val renderParams = structdef {
        field("testSetsId", PredefinedType.long)
        field("classUnderTest", array(PredefinedType.byte))
        field("projectType", PredefinedType.string)
        field("paramNames", array(PredefinedType.byte))
        field("generateUtilClassFile", PredefinedType.bool)
        field("testFramework", PredefinedType.string)
        field("mockFramework", PredefinedType.string)
        field("codegenLanguage", PredefinedType.string)
        field("parameterizedTestSource", PredefinedType.string)
        field("staticsMocking", PredefinedType.string)
        field("forceStaticMocking", array(PredefinedType.byte))
        field("generateWarningsForStaticMocking", PredefinedType.bool)
        field("runtimeExceptionTestsBehaviour", PredefinedType.string)
        field("hangingTestsTimeout", PredefinedType.long)
        field("enableTestsTimeout", PredefinedType.bool)
        field("testClassPackageName", PredefinedType.string)
    }
    val renderResult = structdef {
        field("generatedCode", PredefinedType.string)
        field("utilClassKind", PredefinedType.string.nullable)
    }
    val setupContextParams = structdef {
        field("classpathForUrlsClassloader", immutableList(PredefinedType.string))
    }
    val getSpringBeanDefinitions = structdef {
        field("classpath", array(PredefinedType.string))
        field("config", PredefinedType.string)
        field("fileStorage", array(PredefinedType.string))
        field("profileExpression", PredefinedType.string.nullable)
    }
    val methodDescription = structdef {
        field("name", PredefinedType.string)
        field("containingClass", PredefinedType.string.nullable)
        field("parametersTypes", immutableList(PredefinedType.string.nullable))
    }
    val findMethodsInClassMatchingSelectedArguments = structdef {
        field("classId", array(PredefinedType.byte))
        field("methodDescriptions", immutableList(methodDescription))
    }
    val findMethodsInClassMatchingSelectedResult = structdef {
        field("executableIds", array(PredefinedType.byte))
    }
    val findMethodParamNamesArguments = structdef {
        field("classId", array(PredefinedType.byte))
        field("bySignature", array(PredefinedType.byte))
    }
    val findMethodParamNamesResult = structdef {
        field("paramNames", array(PredefinedType.byte))
    }
    val writeSarifReportArguments = structdef {
        field("testSetsId", PredefinedType.long)
        field("reportFilePath", PredefinedType.string)
        field("generatedTestsCode", PredefinedType.string)
    }
    val generateTestReportArgs = structdef {
        field("eventLogMessage", PredefinedType.string.nullable)
        field("testPackageName", PredefinedType.string.nullable)
        field("isMultiPackage", PredefinedType.bool)
        field("forceMockWarning", PredefinedType.string.nullable)
        field("forceStaticMockWarnings", PredefinedType.string.nullable)
        field("testFrameworkWarning", PredefinedType.string.nullable)
        field("hasInitialWarnings", PredefinedType.bool)
    }
    val generateTestReportResult = structdef {
        field("notifyMessage", PredefinedType.string)
        field("statistics", PredefinedType.string.nullable)
        field("hasWarnings", PredefinedType.bool)
    }
    val beanAdditionalData = structdef {
        field("factoryMethodName", PredefinedType.string)
        field("configClassFqn", PredefinedType.string)
    }
    val beanDefinitionData = structdef {
        field("beanName", PredefinedType.string)
        field("beanTypeFqn", PredefinedType.string)
        field("additionalData", beanAdditionalData.nullable)
    }
    val springAnalyzerResult = structdef {
        field("beanDefinitions", array(beanDefinitionData))
    }

    init {
        call("setupUtContext", setupContextParams, PredefinedType.void).async
        call("getSpringBeanDefinitions", getSpringBeanDefinitions, springAnalyzerResult).async
        call("createTestGenerator", testGeneratorParams, PredefinedType.void).async
        call("isCancelled", PredefinedType.void, PredefinedType.bool).async
        call("generate", generateParams, generateResult).async
        call("render", renderParams, renderResult).async
        call("obtainClassId", PredefinedType.string, array(PredefinedType.byte)).async
        call("findMethodsInClassMatchingSelected", findMethodsInClassMatchingSelectedArguments, findMethodsInClassMatchingSelectedResult).async
        call("findMethodParamNames", findMethodParamNamesArguments, findMethodParamNamesResult).async
        call("writeSarifReport", writeSarifReportArguments, PredefinedType.string).async
        call("generateTestReport", generateTestReportArgs, generateTestReportResult).async
    }
}