package org.utbot.examples.spring.utils

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testing.SpringConfiguration
import org.utbot.testing.TestExecution

val standardSpringTestingConfigurations: List<SpringConfiguration> = listOf(
    SpringConfiguration(CodegenLanguage.JAVA, ParametrizedTestSource.DO_NOT_PARAMETRIZE, TestExecution)
)

val springAdditionalDependencies: Array<Class<*>> = arrayOf(
    JpaRepository::class.java,
    PagingAndSortingRepository::class.java,
)