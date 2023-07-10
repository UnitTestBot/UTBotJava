package org.utbot.spring.dummy

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase

open class DummyPureSpringIntegrationTestClass : DummySpringIntegrationTestClass()

@AutoConfigureTestDatabase
class DummyPureSpringIntegrationTestClassAutoconfigTestDB : DummyPureSpringIntegrationTestClass()
