package org.utbot.testing

import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.context.simple.SimpleApplicationContext
import org.utbot.framework.context.simple.SimpleMockerContext
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockStrategyApi

interface AbstractConfiguration {
    val projectType: ProjectType
    val mockStrategy: MockStrategyApi
    val language: CodegenLanguage
    val parametrizedTestSource: ParametrizedTestSource
    val lastStage: Stage
}

data class Configuration(
    override val language: CodegenLanguage,
    override val parametrizedTestSource: ParametrizedTestSource,
    override val lastStage: Stage,
): AbstractConfiguration {
    override val projectType: ProjectType
        get() = ProjectType.PureJvm

    override val mockStrategy: MockStrategyApi
        get() = MockStrategyApi.defaultItem
}

data class SpringConfiguration(
    override val language: CodegenLanguage,
    override val parametrizedTestSource: ParametrizedTestSource,
    override val lastStage: Stage,
): AbstractConfiguration {
    override val projectType: ProjectType
        get() = ProjectType.Spring

    override val mockStrategy: MockStrategyApi
        get() = MockStrategyApi.springDefaultItem
}

val defaultApplicationContext = SimpleApplicationContext(
    SimpleMockerContext(
        mockFrameworkInstalled = true,
        staticsMockingIsConfigured = true,
    )
)
