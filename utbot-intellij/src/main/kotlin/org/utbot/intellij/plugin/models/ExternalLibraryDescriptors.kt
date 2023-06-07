package org.utbot.intellij.plugin.models

import com.intellij.openapi.roots.ExternalLibraryDescriptor
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription
import org.utbot.intellij.plugin.ui.utils.Version
import org.utbot.intellij.plugin.ui.utils.hasNumericPatch

val ExternalLibraryDescriptor.mavenCoordinates: String
    get() = "$libraryGroupId:$libraryArtifactId:${preferredVersion ?: RepositoryLibraryDescription.ReleaseVersionId}"

val ExternalLibraryDescriptor.id: String
    get() = "$libraryGroupId:$libraryArtifactId"

//TODO: think about using JUnitExternalLibraryDescriptor from intellij-community sources (difficult to install)
fun jUnit4LibraryDescriptor(versionInProject: Version?): ExternalLibraryDescriptor {
    val preferredVersion = if (versionInProject?.hasNumericPatch() == true) versionInProject?.plainText else "4.13.2"
    return ExternalLibraryDescriptor(
        "junit", "junit",
        "4.12", null, preferredVersion
    )
}

fun jUnit5LibraryDescriptor(versionInProject: Version?): ExternalLibraryDescriptor {
    val preferredVersion = if (versionInProject?.hasNumericPatch() == true) versionInProject?.plainText else "5.8.1"
    return ExternalLibraryDescriptor(
        "org.junit.jupiter", "junit-jupiter",
        "5.8.1", null, preferredVersion
    )
}

fun jUnit5ParametrizedTestsLibraryDescriptor(versionInProject: Version?): ExternalLibraryDescriptor {
    val preferredVersion = if (versionInProject?.hasNumericPatch() == true) versionInProject?.plainText else "5.8.1"
    return ExternalLibraryDescriptor(
        "org.junit.jupiter", "junit-jupiter-params",
        "5.8.1", null, preferredVersion
    )
}

fun mockitoCoreLibraryDescriptor(versionInProject: Version?): ExternalLibraryDescriptor {
    val preferredVersion = if (versionInProject?.hasNumericPatch() == true) versionInProject?.plainText else "4.2.0"
    return ExternalLibraryDescriptor(
        "org.mockito", "mockito-core",
        "3.5.0", null, preferredVersion
    )
}

fun springBootTestLibraryDescriptor(versionInProject: Version?): ExternalLibraryDescriptor {
    val preferredVersion = if (versionInProject?.hasNumericPatch() == true) versionInProject?.plainText else "3.0.6"
    return ExternalLibraryDescriptor(
        "org.springframework.boot", "spring-boot-test",
        "2.4.0", null, preferredVersion
    )
}

fun springTestLibraryDescriptor(versionInProject: Version?): ExternalLibraryDescriptor {
    val preferredVersion = if (versionInProject?.hasNumericPatch() == true) versionInProject?.plainText else "6.0.8"
    return ExternalLibraryDescriptor(
        "org.springframework", "spring-test",
        "2.5", null, preferredVersion
    )
}


/**
 * TestNg requires JDK 11 since version 7.6.0
 * For projects with JDK 8 version 7.5 should be installed.
 * See https://groups.google.com/g/testng-users/c/BAFB1vk-kok?pli=1 for more details.
 */
fun testNgNewLibraryDescriptor(versionInProject: Version?): ExternalLibraryDescriptor {
    val preferredVersion = if (versionInProject?.hasNumericPatch() == true) versionInProject?.plainText else "7.6.0"
    return ExternalLibraryDescriptor(
        "org.testng", "testng",
        "7.6.0", null, preferredVersion
    )
}

fun testNgOldLibraryDescriptor() =
    ExternalLibraryDescriptor(
        "org.testng", "testng",
        "7.5", "7.5", "7.5"
    )