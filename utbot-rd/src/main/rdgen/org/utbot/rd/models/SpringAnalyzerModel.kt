@file:Suppress("unused")
package org.utbot.rd.models

import com.jetbrains.rd.generator.nova.*

object SpringAnalyzerRoot : Root()

object SpringAnalyzerProcessModel : Ext(SpringAnalyzerRoot) {
    val springAnalyzerParams = structdef {
        field("classpath", array(PredefinedType.string))
        field("configuration", PredefinedType.string)
        field("fileStorage", PredefinedType.string.nullable)
    }

    val springAnalyzerResult = structdef {
        field("beanTypes", array(PredefinedType.string))
    }

    init {
        call("analyze", springAnalyzerParams, springAnalyzerResult).async
    }
}
