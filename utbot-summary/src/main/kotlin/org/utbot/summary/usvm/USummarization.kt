package org.utbot.summary.usvm

import org.utbot.framework.plugin.api.UtMethodTestSet
import mu.KotlinLogging
import org.utbot.common.measureTime
import org.utbot.common.info
import org.utbot.framework.SummariesGenerationType.*
import org.utbot.framework.UtSettings.enableDisplayNameGeneration
import org.utbot.framework.UtSettings.enableJavaDocGeneration
import org.utbot.framework.UtSettings.enableTestNamesGeneration
import org.utbot.framework.UtSettings.summaryGenerationType
import org.utbot.summary.InvokeDescription
import org.utbot.summary.MethodDescriptionSource
import org.utbot.summary.Summarization
import java.io.File

private val logger = KotlinLogging.logger {}

/**
USummarization is used to generate summaries for *usvm-sbft*.

To generate summary, use the following settings:
- *SummariesGenerationType == LIGHT*
- *enableTestNamesGeneration = true*
- *enableDisplayNameGeneration = false*
- *enableJavaDocGeneration = true*
 */

fun Collection<UtMethodTestSet>.summarizeAll(sourceFile: File?): List<UtMethodTestSet> =
    logger.info().measureTime({
        "----------------------------------------------------------------------------------------\n" +
                "-------------------Summarization started for ${this.size} test cases--------------------\n" +
                "----------------------------------------------------------------------------------------"
    }) {
        this.map {
            it.summarizeOne(sourceFile)
        }
    }

private fun UtMethodTestSet.summarizeOne(sourceFile: File?): UtMethodTestSet =
    logger.info().measureTime({ "Summarization for ${this.method}" }) {

        if (summaryGenerationType != LIGHT || !enableTestNamesGeneration || enableDisplayNameGeneration || !enableJavaDocGeneration) {
            logger.info {
                "Incorrect settings are used to generate Summaries for usvm-sbft"
            }
            return this
        }

        USummarization(sourceFile, invokeDescriptions = emptyList()).fillSummaries(this)
        return this
    }

class USummarization(sourceFile: File?, invokeDescriptions: List<InvokeDescription>) :
    Summarization(sourceFile, invokeDescriptions) {

    /*
     * Used to prepare methodTestSet for further generation of summaries.
     * In the case of generating tests using USVM, we only need to work with Symbolic tests.
     */
    override fun prepareMethodTestSet(
        testSet: UtMethodTestSet,
        descriptionSource: MethodDescriptionSource
    ): UtMethodTestSet {
        return when (descriptionSource) {
            MethodDescriptionSource.FUZZER -> UtMethodTestSet(
                method = testSet.method,
                executions = emptyList(),
                jimpleBody = testSet.jimpleBody,
                errors = testSet.errors,
                clustersInfo = testSet.clustersInfo
            )

            MethodDescriptionSource.SYMBOLIC -> testSet
        }
    }
}
