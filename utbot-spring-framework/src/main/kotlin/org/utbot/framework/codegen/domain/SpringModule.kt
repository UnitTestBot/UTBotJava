package org.utbot.framework.codegen.domain

abstract class SpringModule(
    val testFrameworkDisplayName: String,
    /**
     * Generation Spring specific tests requires special spring test framework being installed,
     * so we can use `TestContextManager` from `spring-test` to configure test context in
     * spring-analyzer and to run integration tests.
     */
    var testFrameworkInstalled: Boolean = false
) {
    var isInstalled = false

    companion object {
        val allItems: List<SpringModule> get() = listOf(SpringBoot, SpringBeans)

        val installedItems get() = allItems.filter { it.isInstalled }
    }
}

object SpringBeans : SpringModule(
    testFrameworkDisplayName = "spring-test",
)

object SpringBoot : SpringModule(
    testFrameworkDisplayName = "spring-boot-test",
)