package org.utbot.intellij.plugin.models

import com.intellij.openapi.roots.ExternalLibraryDescriptor
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription
import org.utbot.intellij.plugin.ui.utils.Version

val ExternalLibraryDescriptor.mavenCoordinates: String
    get() = "$libraryGroupId:$libraryArtifactId:${preferredVersion ?: RepositoryLibraryDescription.ReleaseVersionId}"

val ExternalLibraryDescriptor.id: String
    get() = "$libraryGroupId:$libraryArtifactId"

//TODO: think about using JUnitExternalLibraryDescriptor from intellij-community sources (difficult to install)
fun jUnit4LibraryDescriptor(versionInProject: Version?) =
    ExternalLibraryDescriptor(
        "junit", "junit",
        "4.12", null, versionInProject?.plainText ?: "4.13.2"
    )

fun jUnit5LibraryDescriptor(versionInProject: Version?) =
    ExternalLibraryDescriptor(
        "org.junit.jupiter", "junit-jupiter",
        "5.8.1", null, versionInProject?.plainText ?: "5.8.1"
    )

fun jUnit5ParametrizedTestsLibraryDescriptor(versionInProject: Version?) =
    ExternalLibraryDescriptor(
        "org.junit.jupiter", "junit-jupiter-params",
        "5.8.1", null, versionInProject?.plainText ?: "5.8.1"
    )

fun mockitoCoreLibraryDescriptor(versionInProject: Version?) =
    ExternalLibraryDescriptor(
        "org.mockito", "mockito-core",
        "3.5.0", null, versionInProject?.plainText ?: "4.2.0"
    )

fun springBootTestLibraryDescriptor(versionInProject: Version?) =
    ExternalLibraryDescriptor(
        "org.springframework.boot", "spring-boot-test",
        "2.4.0", null, versionInProject?.plainText ?: "3.0.6"
    )

fun springTestLibraryDescriptor(versionInProject: Version?) =
    ExternalLibraryDescriptor(
        "org.springframework", "spring-test",
        "2.5", null, versionInProject?.plainText ?: "6.0.8"
    )


/**
 * TestNg requires JDK 11 since version 7.6.0
 * For projects with JDK 8 version 7.5 should be installed.
 * See https://groups.google.com/g/testng-users/c/BAFB1vk-kok?pli=1 for more details.
 */
fun testNgNewLibraryDescriptor(versionInProject: Version?) =
    ExternalLibraryDescriptor(
        "org.testng", "testng",
        "7.6.0", null, versionInProject?.plainText ?: "7.6.0"
    )

fun testNgOldLibraryDescriptor() =
    ExternalLibraryDescriptor(
        "org.testng", "testng",
        "7.5", "7.5", "7.5"
    )