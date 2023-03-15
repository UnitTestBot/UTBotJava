package application.utils

import utils.PathsUtils
import java.io.File
import java.io.IOException

class FakeFileManager(private val fakeFilesList: List<String>) {

    fun createFakeFiles() {
        for (fileName in fakeFilesList) {
            val fakeXmlFileAbsolutePath = PathsUtils.createFakeFilePath(fileName)

            try {
                File(fakeXmlFileAbsolutePath).createNewFile()
            } catch (e: IOException) {
                println("Fake xml file creation failed with exception $e")
            }

        }
    }

    fun deleteFakeFiles() {
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