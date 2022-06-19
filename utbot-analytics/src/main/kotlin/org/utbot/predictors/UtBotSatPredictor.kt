package org.utbot.predictors

import org.utbot.analytics.IUtBotSatPredictor
import org.utbot.analytics.NeuroSatData
import org.utbot.analytics.UtBotAbstractPredictor
import org.utbot.engine.pc.UtExpression
import org.utbot.features.UtExpressionGraphExtraction
import org.utbot.features.UtExpressionId
import org.utbot.utils.BinaryUtil
import java.io.File
import java.io.FileOutputStream

@Suppress("unused")
class UtBotSatPredictor : UtBotAbstractPredictor<Iterable<UtExpression>, NeuroSatData>,
    IUtBotSatPredictor<Iterable<UtExpression>> {

    private var counter = 1
    private var folder = "logs/GRAPHS"

    init {
        File("${folder}/graphs").mkdirs()
        File("${folder}/SAT.txt").printWriter().use { out ->
            out.println("ID,SAT,TIME")
        }
    }

    override fun provide(input: Iterable<UtExpression>, expectedResult: NeuroSatData, actualResult: NeuroSatData) {
        val extractor = UtExpressionGraphExtraction()
        File("${folder}/graphs/$counter").mkdir()
        File("${folder}/graphs/$counter/edges.txt").printWriter().use { out ->
            out.print(extractor.extractGraph(input))
        }
        File("${folder}/graphs/$counter/vertexesType.txt").printWriter().use { out ->
            out.println("UUID,TYPE")
            out.print(extractor.expressionIdsCache.keys.map {
                "${extractor.getID(it)},${
                    BinaryUtil.binaryExpressionString(UtExpressionId().getID(it))
                }"
            }.joinToString("\n"))
        }
        File("${folder}/graphs/$counter/vertexesValues.txt").printWriter().use { out ->
            out.println("UUID,VALUE")
            out.print(extractor.literalValues.keys.map { "${extractor.getID(it)},${extractor.literalValues[it]}" }
                .joinToString("\n"))
        }

        FileOutputStream("${folder}/SAT.txt", true).bufferedWriter().use { out ->
            out.write("$counter,${actualResult.status},${actualResult.time}")
            out.newLine()
        }

        counter++
    }

    override fun terminate() {
    }
}