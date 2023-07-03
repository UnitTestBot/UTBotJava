package org.utbot.spring.context

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles(/* fills dynamically */)
@ContextConfiguration(/* fills dynamically */)
@Transactional(isolation = Isolation.SERIALIZABLE)
@AutoConfigureTestDatabase
open class BaseDummyTestClass {
    fun dummyTestMethod() {}
}