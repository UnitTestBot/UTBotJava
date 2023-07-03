package org.utbot.spring.context

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper
import org.springframework.test.context.BootstrapWith

@BootstrapWith(SpringBootTestContextBootstrapper::class)
class SpringBootDummyTestClass : BaseDummyTestClass()
