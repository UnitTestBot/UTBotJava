package org.utbot.monitoring

import kotlinx.serialization.Serializable
import org.utbot.contest.GlobalStats
import org.utbot.contest.StatsForClass

@Serializable
data class MonitoringReport(
    val parameters: MonitoringParameters,
    val targets: List<TargetReport>,
) {

    constructor(parameters: MonitoringParameters, stats: GlobalStats) : this(
        parameters,
        stats.projectStats.map { projectStats ->
            TargetReport(
                projectStats.project,
                projectStats.statsForClasses.map {
                    ClassReport(it.className, ClassMetricsReport(it))
                }
            )
        }
    )
}

@Serializable
data class MonitoringParameters(
    val fuzzing_ratio: Double,
    val class_timeout_sec: Int,
    val run_timeout_min: Int,
)

@Serializable
data class TargetReport(
    val target: String,
    val summarised_metrics: SummarisedMetricsReport,
    val metrics_by_class: List<ClassReport>
) {
    constructor(target: String, metrics_by_class: List<ClassReport>): this(
        target,
        SummarisedMetricsReport(metrics_by_class),
        metrics_by_class
    )
}

@Serializable
data class ClassReport(
    val class_name: String,
    val metrics: ClassMetricsReport
)

private fun Int.cover(total: Int): Double =
    if (total == 0) 0.0 else this.toDouble() / total

@Serializable
data class ClassMetricsReport(
    val testcases_generated: Int,
    val failed_to_compile: Boolean,
    val canceled_by_timeout: Boolean,
    val total_methods_in_class: Int,
    val methods_with_at_least_one_testcase_generated: Int,
    val methods_with_at_least_one_exception: Int,
    val methods_without_any_tests_and_exceptions: Int,
    val covered_bytecode_instructions_in_class: Int,
    val covered_bytecode_instructions_in_class_by_fuzzing: Int,
    val covered_bytecode_instructions_in_class_by_concolic: Int,
    val total_bytecode_instructions_in_class: Int,
    val bytecode_instructions_coverage_in_class: Double = covered_bytecode_instructions_in_class.cover(total_bytecode_instructions_in_class),
    val bytecode_instructions_coverage_in_class_by_fuzzing: Double = covered_bytecode_instructions_in_class_by_fuzzing.cover(total_bytecode_instructions_in_class),
    val bytecode_instructions_coverage_in_class_by_concolic: Double = covered_bytecode_instructions_in_class_by_concolic.cover(total_bytecode_instructions_in_class)
) {
    constructor(statsForClass: StatsForClass) : this(
        testcases_generated = statsForClass.testcasesGenerated,
        failed_to_compile = statsForClass.failedToCompile,
        canceled_by_timeout = statsForClass.canceledByTimeout,
        total_methods_in_class = statsForClass.methodsCount,
        methods_with_at_least_one_testcase_generated = statsForClass.statsForMethods.count { it.testsGeneratedCount > 0 },
        methods_with_at_least_one_exception = statsForClass.methodsWithAtLeastOneException,
        methods_without_any_tests_and_exceptions = statsForClass.statsForMethods.count { it.isSuspicious },
        covered_bytecode_instructions_in_class = statsForClass.getCoverageInfo().covered,
        covered_bytecode_instructions_in_class_by_fuzzing = statsForClass.getFuzzedCoverageInfo().covered,
        covered_bytecode_instructions_in_class_by_concolic = statsForClass.getConcolicCoverageInfo().covered,
        total_bytecode_instructions_in_class = statsForClass.coverage.totalInstructions.toInt()
    )
}

@Serializable
data class SummarisedMetricsReport(
    val total_classes: Int,
    val testcases_generated: Int,
    val classes_failed_to_compile: Int,
    val classes_canceled_by_timeout: Int,
    val total_methods: Int,
    val methods_with_at_least_one_testcase_generated: Int,
    val methods_with_at_least_one_exception: Int,
    val methods_without_any_tests_and_exceptions: Int,
    val covered_bytecode_instructions: Int,
    val covered_bytecode_instructions_by_fuzzing: Int,
    val covered_bytecode_instructions_by_concolic: Int,
    val total_bytecode_instructions: Int,
    val bytecode_instructions_coverage: Double = covered_bytecode_instructions.cover(total_bytecode_instructions),
    val bytecode_instructions_coverage_by_fuzzing: Double = covered_bytecode_instructions_by_fuzzing.cover(total_bytecode_instructions),
    val bytecode_instructions_coverage_by_concolic: Double = covered_bytecode_instructions_by_concolic.cover(total_bytecode_instructions),
    val averaged_bytecode_instruction_coverage_by_classes: Double
) {
    constructor(targets: Collection<ClassReport>) : this(
        total_classes = targets.size,
        testcases_generated = targets.sumOf { it.metrics.testcases_generated },
        classes_failed_to_compile = targets.count { it.metrics.failed_to_compile },
        classes_canceled_by_timeout = targets.count { it.metrics.canceled_by_timeout },
        total_methods = targets.sumOf { it.metrics.total_methods_in_class },
        methods_with_at_least_one_testcase_generated = targets.sumOf { it.metrics.methods_with_at_least_one_testcase_generated },
        methods_with_at_least_one_exception = targets.sumOf { it.metrics.methods_with_at_least_one_exception },
        methods_without_any_tests_and_exceptions = targets.sumOf { it.metrics.methods_without_any_tests_and_exceptions },
        covered_bytecode_instructions = targets.sumOf { it.metrics.covered_bytecode_instructions_in_class },
        covered_bytecode_instructions_by_fuzzing = targets.sumOf { it.metrics.covered_bytecode_instructions_in_class_by_fuzzing },
        covered_bytecode_instructions_by_concolic = targets.sumOf { it.metrics.covered_bytecode_instructions_in_class_by_concolic },
        total_bytecode_instructions = targets.sumOf { it.metrics.total_bytecode_instructions_in_class },
        averaged_bytecode_instruction_coverage_by_classes = targets.map { it.metrics.bytecode_instructions_coverage_in_class }.average().fixNaN()
    )
}

private fun Double.fixNaN(): Double = if (isNaN() || isInfinite()) {
    0.0
} else {
    this
}
