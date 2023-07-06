package org.utbot.testing

import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.plugin.api.CodegenLanguage

abstract class AbstractConfiguration(
    val projectType: ProjectType,
    open val language: CodegenLanguage,
    open val parametrizedTestSource: ParametrizedTestSource,
    open val lastStage: Stage,
)

data class Configuration(
    override val language: CodegenLanguage,
    override val parametrizedTestSource: ParametrizedTestSource,
    override val lastStage: Stage,
): AbstractConfiguration(ProjectType.PureJvm, language, parametrizedTestSource, lastStage)

data class SpringConfiguration(
    override val language: CodegenLanguage,
    override val parametrizedTestSource: ParametrizedTestSource,
    override val lastStage: Stage,
): AbstractConfiguration(ProjectType.Spring, language, parametrizedTestSource, lastStage)