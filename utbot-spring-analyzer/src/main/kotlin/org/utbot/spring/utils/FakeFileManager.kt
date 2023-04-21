package org.utbot.spring.utils

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import java.io.File
import java.io.IOException

private val logger = getLogger<FakeFileManager>()

class FakeFileManager(private val fakeFilesList: List<String>) {

    fun createTempFiles() {
        for (fileName in fakeFilesList) {
            val fakeXmlFileAbsolutePath = PathsUtils.createFakeFilePath(fileName)

            try {
                File(fakeXmlFileAbsolutePath).createNewFile()
            } catch (e: IOException) {
                logger.info { "Fake xml file creation failed with exception $e" }
            }

        }
    }

    fun deleteTempFiles() {
        for (fileName in fakeFilesList) {
            val fakeXmlFileAbsolutePath = PathsUtils.createFakeFilePath(fileName)

            try {
                File(fakeXmlFileAbsolutePath).delete()
            } catch (e: IOException) {
                logger.info { "Fake xml file deletion failed with exception $e" }
            }

        }
    }
}