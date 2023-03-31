package org.utbot.spring.utils

import java.io.File
import java.io.IOException

class FakeFileManager(private val fakeFilesList: List<String>) {

    fun createTempFiles() {
        for (fileName in fakeFilesList) {
            val fakeXmlFileAbsolutePath = PathsUtils.createFakeFilePath(fileName)

            try {
                File(fakeXmlFileAbsolutePath).createNewFile()
            } catch (e: IOException) {
                println("Fake xml file creation failed with exception $e")
            }

        }
    }

    fun deleteTempFiles() {
        for (fileName in fakeFilesList) {
            val fakeXmlFileAbsolutePath = PathsUtils.createFakeFilePath(fileName)

            try {
                File(fakeXmlFileAbsolutePath).delete()
            } catch (e: IOException) {
                println("Fake xml file deletion failed with exception $e")
            }

        }
    }
}