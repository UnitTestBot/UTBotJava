package org.utbot.intellij.plugin.util

import com.intellij.openapi.application.ApplicationManager

fun assertIsDispatchThread() {
    ApplicationManager.getApplication().assertIsDispatchThread()
}

fun assertIsWriteThread() {
    ApplicationManager.getApplication().isWriteThread()
}

fun assertIsReadAccessAllowed() {
    ApplicationManager.getApplication().assertReadAccessAllowed()
}

fun assertIsWriteAccessAllowed() {
    ApplicationManager.getApplication().assertWriteAccessAllowed()
}

fun assertIsNonDispatchThread() {
    ApplicationManager.getApplication().assertIsNonDispatchThread()
}