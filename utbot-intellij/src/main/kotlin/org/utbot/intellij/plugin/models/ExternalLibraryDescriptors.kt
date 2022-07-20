package org.utbot.intellij.plugin.models

import com.intellij.openapi.roots.ExternalLibraryDescriptor

//TODO: think about using JUnitExternalLibraryDescriptor from intellij-community sources (difficult to install)
fun jUnit4LibraryDescriptor(versionInProject: String?) =
    ExternalLibraryDescriptor("junit", "junit", "4.12", null, versionInProject ?: "4.13.2")

fun jUnit5LibraryDescriptor(versionInProject: String?) =
    ExternalLibraryDescriptor("org.junit.jupiter", "junit-jupiter", "5.8.1", null, versionInProject ?: "5.8.1")

fun testNgLibraryDescriptor(versionInProject: String?) =
    ExternalLibraryDescriptor("org.testng", "testng", "6.8.8", null, versionInProject ?: "6.9.6")

fun mockitoCoreLibraryDescriptor(versionInProject: String?) =
    ExternalLibraryDescriptor("org.mockito", "mockito-core", "3.5.0", "4.2.0", versionInProject ?: "4.2.0")