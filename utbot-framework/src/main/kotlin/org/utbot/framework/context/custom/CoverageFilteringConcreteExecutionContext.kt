package org.utbot.framework.context.custom

import mu.KotlinLogging
import org.utbot.common.hasOnClasspath
import org.utbot.common.tryLoadClass
import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.util.utContext
import java.io.File
import java.net.URLClassLoader

/**
 * Decorator of [delegateContext] that filters coverage before test set minimization
 * (see [transformExecutionsBeforeMinimization]) to avoid generating too many tests that
 * only increase coverage of third party libraries.
 *
 * This implementation:
 * - always keeps instructions that are in class under test (even if other rules say otherwise)
 * - filters out instructions in classes marked with annotations from [annotationsToIgnoreCoverage]
 * - filters out instructions from classes that are not found in `classpathToIncludeCoverageFrom`
 *
 * Finally, if [keepOriginalCoverageOnEmptyFilteredCoverage] is `true` we restore original coverage
 * for executions whose coverage becomes empty after filtering.
 */
class CoverageFilteringConcreteExecutionContext(
    private val delegateContext: ConcreteExecutionContext,
    classpathToIncludeCoverageFrom: String,
    private val annotationsToIgnoreCoverage: Set<ClassId>,
    private val keepOriginalCoverageOnEmptyFilteredCoverage: Boolean,
) : ConcreteExecutionContext by delegateContext {
    private val urlsToIncludeCoverageFrom = classpathToIncludeCoverageFrom.split(File.pathSeparator)
        .map { File(it).toURI().toURL() }
        .toTypedArray()

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun transformExecutionsBeforeMinimization(
        executions: List<UtExecution>,
        classUnderTestId: ClassId
    ): List<UtExecution> {
        val annotationsToIgnoreCoverage =
            annotationsToIgnoreCoverage.mapNotNull { utContext.classLoader.tryLoadClass(it.name) }

        val classLoaderToIncludeCoverageFrom = URLClassLoader(urlsToIncludeCoverageFrom, null)

        val classesToIncludeCoverageFromCache = mutableMapOf<String, Boolean>()

        return executions.map { execution ->
            val coverage = execution.coverage ?: return@map execution

            val filteredCoveredInstructions =
                coverage.coveredInstructions
                    .filter { instruction ->
                        val instrClassName = instruction.className

                        classesToIncludeCoverageFromCache.getOrPut(instrClassName) {
                            instrClassName == classUnderTestId.name ||
                                    (classLoaderToIncludeCoverageFrom.hasOnClasspath(instrClassName) &&
                                            !hasAnnotations(instrClassName, annotationsToIgnoreCoverage))
                        }
                    }
                    .ifEmpty {
                        if (keepOriginalCoverageOnEmptyFilteredCoverage) {
                            logger.warn("Execution covered instruction list became empty. Proceeding with not filtered instruction list.")
                            coverage.coveredInstructions
                        } else {
                            logger.warn("Execution covered instruction list became empty. Proceeding with empty coverage.")
                            emptyList()
                        }
                    }

            execution.copy(
                coverage = coverage.copy(
                    coveredInstructions = filteredCoveredInstructions
                )
            )
        }
    }

    private fun hasAnnotations(className: String, annotations: List<Class<*>>): Boolean =
        utContext
            .classLoader
            .loadClass(className)
            .annotations
            .any { existingAnnotation ->
                annotations.any { annotation ->
                    annotation.isInstance(existingAnnotation)
                }
            }
}