package org.utbot.spring.dummy

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper
import org.springframework.test.context.BootstrapWith

@BootstrapWith(SpringBootTestContextBootstrapper::class)
class DummySpringBootIntegrationTestClass : DummySpringIntegrationTestClass()
