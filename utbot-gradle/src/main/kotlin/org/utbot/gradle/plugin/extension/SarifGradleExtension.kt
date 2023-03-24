package org.utbot.gradle.plugin.extension

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

/**
 * The class is used to configure the gradle plugin.
 * [Documentation](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:getting_input_from_the_build)
 *
 * See [SarifGradleExtensionProvider] for default values
 */
abstract class SarifGradleExtension {

    /**
     * Classes for which the SARIF report will be created.
     * Uses all classes from the user project if this list is empty.
     */
    @get:Input
    abstract val targetClasses: ListProperty<String>

    /**
     * Absolute path to the root of the relative paths in the SARIF report.
     */
    @get:Input
    abstract val projectRoot: Property<String>

    /**
     * Relative path (against module root) to the root of the generated tests.
     */
    @get:Input
    abstract val generatedTestsRelativeRoot: Property<String>

    /**
     * Relative path (against module root) to the root of the SARIF reports.
     */
    @get:Input
    abstract val sarifReportsRelativeRoot: Property<String>

    /**
     * Mark the directory with generated tests as `test sources root` or not.
     */
    @get:Input
    abstract val markGeneratedTestsDirectoryAsTestSourcesRoot: Property<Boolean>

    /**
     * Generate tests for private methods or not.
     */
    @get:Input
    abstract val testPrivateMethods: Property<Boolean>

    /**
     * Can be one of: 'purejvm', 'spring', 'python', 'javascript`.
     */
    @get:Input
    abstract val projectType: Property<String>

    /**
     * Can be one of: 'junit4', 'junit5', 'testng'.
     */
    @get:Input
    abstract val testFramework: Property<String>

    /**
     * Can be one of: 'mockito'.
     */
    @get:Input
    abstract val mockFramework: Property<String>

    /**
     * Maximum tests generation time for one class (in milliseconds).
     */
    @get:Input
    abstract val generationTimeout: Property<Long>

    /**
     * Can be one of: 'java', 'kotlin'.
     */
    @get:Input
    abstract val codegenLanguage: Property<String>

    /**
     * Can be one of: 'no-mocks', 'other-packages', 'other-classes'.
     */
    @get:Input
    abstract val mockStrategy: Property<String>

    /**
     * Can be one of: 'do-not-mock-statics', 'mock-statics'.
     */
    @get:Input
    abstract val staticsMocking: Property<String>

    /**
     * Can be one of: 'force', 'do-not-force'.
     */
    @get:Input
    abstract val forceStaticMocking: Property<String>

    /**
     * Classes to force mocking theirs static methods and constructors.
     */
    @get:Input
    abstract val classesToMockAlways: ListProperty<String>
}

/*
Example configuration:

sarifReport {
    targetClasses = ['com.abc.Main']
    projectRoot = 'C:/.../SomeDirectory'
    generatedTestsRelativeRoot = 'build/generated/test'
    sarifReportsRelativeRoot = 'build/generated/sarif'
    markGeneratedTestsDirectoryAsTestSourcesRoot = true
    testFramework = 'junit5'
    mockFramework = 'mockito'
    generationTimeout = 60 * 1000L
    codegenLanguage = 'java'
    mockStrategy = 'no-mocks'
    staticsMocking = 'do-not-mock-statics'
    forceStaticMocking = 'force'
    classesToMockAlways = ['org.utbot.api.mock.UtMock']
}
 */
