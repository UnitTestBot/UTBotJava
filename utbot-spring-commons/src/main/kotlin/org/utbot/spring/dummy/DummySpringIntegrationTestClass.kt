package org.utbot.spring.dummy

import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles(/* fills dynamically */)
@ContextConfiguration(/* fills dynamically */)
@Transactional(isolation = Isolation.SERIALIZABLE)
@WithMockUser
abstract class DummySpringIntegrationTestClass {
    @javax.persistence.PersistenceContext
    @jakarta.persistence.PersistenceContext
    lateinit var entityManager: Any

    fun dummyTestMethod() {}
}