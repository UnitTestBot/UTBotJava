package org.utbot.intellij.plugin.ui.utils

/**
 * Describes the version of a library.
 * Contains three standard components: major, minor and patch.
 *
 * Major and minor components are always numbers, while patch
 * may contain a number with some postfix like -RELEASE.
 *
 * Origin is an optional plain text that was used to parse a version.
 */
data class Version(
    val major: Int,
    val minor: Int,
    val patch: String,
    val plainText: String? = null,
)

fun String.parseVersion(): Version? {
    val lastSemicolon = lastIndexOf(':')
    val versionComponents = substring(lastSemicolon + 1).split('.')

    if (versionComponents.size != 3) {
        return null
    }

    val major = versionComponents[0].toIntOrNull() ?: return null
    val minor = versionComponents[1].toIntOrNull() ?: return null
    val patch = versionComponents[2]

    return Version(major, minor, patch, plainText = this)
}

fun Version.isCompatibleWith(another: Version): Boolean {

    fun Version.hasNumericPatch(): Boolean = patch.toIntOrNull() != null

    //Non-numeric versions can't be compared to each other, so we cannot be sure that current is compatible
    if (!hasNumericPatch() || !another.hasNumericPatch()) {
        return false
    }

    return major > another.major ||
            major == another.major && minor > another.minor ||
            major == another.major && minor == another.minor && patch.toInt() >= another.patch.toInt()
}
