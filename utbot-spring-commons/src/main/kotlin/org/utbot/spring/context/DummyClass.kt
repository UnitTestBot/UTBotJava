package org.utbot.spring.context

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.BootstrapWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional


@ActiveProfiles(/* fills dynamically */)
@ContextConfiguration(/* fills dynamically */)
@Transactional
@AutoConfigureTestDatabase
@BootstrapWith(SpringBootTestContextBootstrapper::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
open class DummyClass {
    fun dummyMethod() {}
}