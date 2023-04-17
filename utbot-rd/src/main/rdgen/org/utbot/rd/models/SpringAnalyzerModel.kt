@file:Suppress("unused")
package org.utbot.rd.models

import com.jetbrains.rd.generator.nova.*

object SpringAnalyzerRoot : Root()

object SpringAnalyzerProcessModel : Ext(SpringAnalyzerRoot) {
    val springAnalyzerParams = structdef {
        field("configuration", PredefinedType.string)
        field("fileStorage", array(PredefinedType.string))
    }

    val springAnalyzerResult = structdef {
        field("beanTypes", array(PredefinedType.string))
    }

    init {
        call("analyze", springAnalyzerParams, springAnalyzerResult).async
    }
}
