@file:Suppress("unused")
package org.utbot.rd.models

import com.jetbrains.rd.generator.nova.*

object SpringAnalyzerRoot : Root()

object SpringAnalyzerProcessModel : Ext(SpringAnalyzerRoot) {
    val springAnalyzerParams = structdef {
        field("configuration", PredefinedType.string)
        field("fileStorage", array(PredefinedType.string))
        field("profileExpression", PredefinedType.string.nullable)
    }

    val beanAdditionalData = structdef {
        field("factoryMethodName", PredefinedType.string)
        field("parameterTypes", immutableList(PredefinedType.string))
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
        call("analyze", springAnalyzerParams, springAnalyzerResult).async
    }
}
