package org.utbot.spring.utils

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper
import org.springframework.data.repository.CrudRepository
import org.springframework.web.bind.annotation.RequestMapping

object DependencyUtils {
    val isSpringDataOnClasspath = try {
        CrudRepository::class.java.name
        true
    } catch (e: Throwable) {
        false
    }

    val isSpringWebOnClasspath = try {
        RequestMapping::class.java.name
        true
    } catch (e: Throwable) {
        false
    }

    val isSpringBootTestOnClasspath = try {
        SpringBootTestContextBootstrapper::class.java.name
        true
    } catch (e: Throwable) {
        false
    }
}
