package org.utbot.intellij.plugin.util

import com.intellij.openapi.application.ApplicationManager

fun assertIsDispatchThread() {
    ApplicationManager.getApplication().assertIsDispatchThread()
}

fun assertIsWriteThread() {
    ApplicationManager.getApplication().isWriteThread()
}

fun assertReadAccessAllowed() {
    ApplicationManager.getApplication().assertReadAccessAllowed()
}

fun assertWriteAccessAllowed() {
    ApplicationManager.getApplication().assertWriteAccessAllowed()
}

fun assertIsNonDispatchThread() {
    ApplicationManager.getApplication().assertIsNonDispatchThread()
}

fun assertReadAccessNotAllowed() {
    ApplicationManager.getApplication().assertReadAccessNotAllowed()
}