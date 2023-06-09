package org.utbot.intellij.plugin.ui.utils

/**
 * Describes the version of a library.
 * Contains three standard components: major, minor and patch.
 *
 * Major and minor components are always numbers, while patch
 * may contain a number with some postfix like -RELEASE.
 *
 * Sometimes patch is empty, e.g. for TestNg 7.5 version.
 *
 * @param plainText is optional and represents whole version as text.
 */
data class Version(
    val major: Int,
    val minor: Int,
    val patch: String,
    val plainText: String? = null,
) {
    fun isCompatibleWith(another: Version): Boolean {
        //Non-numeric versions can't be compared to each other, so we cannot be sure that current is compatible
        if (!hasNumericOrEmptyPatch() || !hasNumericOrEmptyPatch()) {
            return false
        }

        return major > another.major ||
                major == another.major && minor > another.minor ||
                major == another.major && minor == another.minor &&
                (another.patch.isEmpty() || patch.isNotEmpty() && patch.toInt() >= another.patch.toInt())
    }

    fun hasNumericOrEmptyPatch(): Boolean = patch.isEmpty() || patch.toIntOrNull() != null
}

fun String.parseVersion(): Version? {
    val lastSemicolon = lastIndexOf(':')
    val versionText = substring(lastSemicolon + 1)
    val versionComponents = versionText.split('.')

    // Components must be: major, minor and (optional) patch
    if (versionComponents.size < 2 || versionComponents.size > 3) {
        return null
    }

    val major = versionComponents[0].toIntOrNull() ?: return null
    val minor = versionComponents[1].toIntOrNull() ?: return null
    val patch = if (versionComponents.size == 3) versionComponents[2] else ""

    return Version(major, minor, patch, versionText)
}