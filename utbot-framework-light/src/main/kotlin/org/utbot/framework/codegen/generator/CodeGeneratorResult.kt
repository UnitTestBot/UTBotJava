package org.utbot.framework.codegen.generator

import org.utbot.framework.codegen.reports.TestsGenerationReport
import org.utbot.framework.codegen.tree.ututils.UtilClassKind

/**
 * @property generatedCode the source code of the test class
 * @property testsGenerationReport some info about test generation process
 * @property utilClassKind the kind of util class if it is required, otherwise - null
 */
data class CodeGeneratorResult(
    val generatedCode: String,
    val testsGenerationReport: TestsGenerationReport,
    // null if no util class needed, e.g. when we are generating utils directly into test class
    val utilClassKind: UtilClassKind? = null,
)