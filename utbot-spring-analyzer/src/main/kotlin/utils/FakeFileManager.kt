package application.utils

import utils.Paths
import java.io.File
import kotlin.io.path.Path

class FakeFileManager(private val fakeFilesList: List<String>) {

    private val buildResourcesPath =
        Path(this.javaClass.classLoader.getResource(Paths.GAG_FILE)!!.path).parent.toString()

    fun createFakeFiles() {
        for (fileName in fakeFilesList) {
            if(fileName == Paths.EMPTY_FILENAME)continue
            val fakeXmlFileAbsolutePath = getFakeFilePath(fileName)
            File(fakeXmlFileAbsolutePath).createNewFile()
        }
    }

    fun deleteFakeFiles() {
        for (fileName in fakeFilesList) {
            if(fileName == Paths.EMPTY_FILENAME)continue
            val fakeXmlFileAbsolutePath = getFakeFilePath(fileName)
            File(fakeXmlFileAbsolutePath).delete()
        }
    }

    fun getFakeFilePath(fileName: String): String {
        return Path(buildResourcesPath, "fake_${Path(fileName).fileName}").toString()
    }
}