package org.utbot.framework.codegen.domain

enum class SpringModule(
    val testFrameworkDisplayName: String,
) {
    SPRING_BEANS(
        testFrameworkDisplayName = "spring-test",
    ),
    SPRING_BOOT(
        testFrameworkDisplayName = "spring-boot-test",
    );

    var isInstalled = false
    /**
     * Generation Spring specific tests requires special spring test framework being installed,
     * so we can use `TestContextManager` from `spring-test` to configure test context in
     * spring-analyzer and to run integration tests.
     */
    var testFrameworkInstalled: Boolean = false

    companion object {
        val installedItems get() = values().filter { it.isInstalled }
    }
}
