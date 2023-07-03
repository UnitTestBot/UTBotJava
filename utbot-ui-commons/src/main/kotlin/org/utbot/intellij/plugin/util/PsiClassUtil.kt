package org.utbot.intellij.plugin.util

import com.intellij.psi.PsiClass
import org.utbot.intellij.plugin.models.packageName


/**
 * Used to build binary name from canonical name
 * in a similar form which could be obtained by [java.lang.Class.getName] method.
 *
 * E.g. ```org.example.OuterClass.InnerClass.InnerInnerClass``` -> ```org.example.OuterClass$InnerClass$InnerInnerClass```
 */
fun PsiClass.binaryName(): String =
    if (packageName.isEmpty()) {
        qualifiedName?.replace(".", "$") ?: ""
    } else {
        val name =
            qualifiedName
                ?.substringAfter("$packageName.")
                ?.replace(".", "$")
                ?: error("Binary name construction failed: unable to get qualified name of $this")
        "$packageName.$name"
    }
