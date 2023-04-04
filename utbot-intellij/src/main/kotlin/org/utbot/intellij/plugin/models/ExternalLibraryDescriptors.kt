package org.utbot.intellij.plugin.models

import com.intellij.openapi.roots.ExternalLibraryDescriptor
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription

val ExternalLibraryDescriptor.mavenCoordinates: String
    get() = "$libraryGroupId:$libraryArtifactId:${preferredVersion ?: RepositoryLibraryDescription.ReleaseVersionId}"

val ExternalLibraryDescriptor.id: String
    get() = "$libraryGroupId:$libraryArtifactId"

//TODO: think about using JUnitExternalLibraryDescriptor from intellij-community sources (difficult to install)
fun jUnit4LibraryDescriptor(versionInProject: String?) =
    ExternalLibraryDescriptor("junit", "junit", "4.12", null, versionInProject ?: "4.13.2")

fun jUnit5LibraryDescriptor(versionInProject: String?) =
    ExternalLibraryDescriptor("org.junit.jupiter", "junit-jupiter", "5.8.1", null, versionInProject ?: "5.8.1")

fun jUnit5ParametrizedTestsLibraryDescriptor(versionInProject: String?) =
    ExternalLibraryDescriptor("org.junit.jupiter", "junit-jupiter-params", "5.8.1", null, versionInProject ?: "5.8.1")

fun mockitoCoreLibraryDescriptor(versionInProject: String?) =
    ExternalLibraryDescriptor("org.mockito", "mockito-core", "3.5.0", null, versionInProject ?: "4.2.0")

/**
 * TestNg requires JDK 11 since version 7.6.0
 * For projects with JDK 8 version 7.5 should be installed.
 * See https://groups.google.com/g/testng-users/c/BAFB1vk-kok?pli=1 for more details.
 */

fun testNgNewLibraryDescriptor(versionInProject: String?) =
    ExternalLibraryDescriptor("org.testng", "testng", "7.6.0", null, versionInProject ?: "7.6.0")

fun testNgOldLibraryDescriptor() =
    ExternalLibraryDescriptor("org.testng", "testng", "7.5", "7.5", "7.5")