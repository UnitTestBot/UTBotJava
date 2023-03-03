package org.utbot.intellij.plugin.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import java.io.File
import java.nio.charset.Charset
import org.utbot.framework.UtSettings

fun showSettingsEditor(project: Project, key: String? = null) {
    val ioFile = File(UtSettings.getPath())
    ApplicationManager.getApplication().executeOnPooledThread {
        var logicalLine: Int = -1
        key?.let {
            logicalLine = ioFile.readLines(Charset.defaultCharset())
                .indexOfFirst { s -> s.startsWith("$key=") or s.startsWith("#$key=") }
        }
        VfsUtil.findFileByIoFile(ioFile, true)?.let {
            val descriptor = if (logicalLine != -1) {
                OpenFileDescriptor(project, it, logicalLine, 0)
            } else {
                OpenFileDescriptor(project, it)
            }
            ApplicationManager.getApplication().invokeLater {
                FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
            }
        }
    }
}
