package org.utbot.engine.z3

import com.microsoft.z3.Context
import com.microsoft.z3.Global
import org.utbot.common.FileUtil
import java.io.File

abstract class Z3Initializer : AutoCloseable {
    private val contextDelegate = lazy {
        Context().also {
//            Global.setParameter("smt.core.minimize", "true")
            Global.setParameter("rewriter.hi_fp_unspecified", "true")
            Global.setParameter("parallel.enable", "true")
            Global.setParameter("parallel.threads.max", "4")
        }
    }
    protected val context: Context by contextDelegate

    override fun close() {
        if (contextDelegate.isInitialized()) {
            context.close()
        }
    }

    companion object {
        private val libraries = listOf("libz3", "libz3java")
        private val vcWinLibrariesToLoadBefore = listOf("vcruntime140", "vcruntime140_1")
        private val supportedArchs = setOf("amd64", "x86_64", "aarch64")
        private val initializeCallback by lazy {
            System.setProperty("z3.skipLibraryLoad", "true")
            val archProperty = System.getProperty("os.arch")
            require(archProperty in supportedArchs) { "Not supported arch: $archProperty" }

            val osProperty = System.getProperty("os.name").lowercase()
            val (os, ext, allLibraries) = when {
                osProperty.startsWith("windows") -> Triple("windows", ".dll", vcWinLibrariesToLoadBefore + libraries)
                osProperty.startsWith("linux") -> Triple("linux", ".so", libraries)
                osProperty.startsWith("mac") -> Triple("mac", ".dylib", libraries)
                else -> error("Unknown OS: $osProperty")
            }

            val arch = if (archProperty == "aarch64") "arm" else "x64"
            val z3DistFolder = "lib/$os/$arch/z3"

            val libZ3FilesUrl = Z3Initializer::class.java
                .classLoader
                .getResource("$z3DistFolder/libz3$ext")
                    ?: error("Can't find native library '$z3DistFolder/libz3$ext' in ${System.getProperty("java.class.path")}")
            // can't take resource of parent folder right here because in obfuscated jar parent folder
            // can be missed (e.g., in case if obfuscation was applied)

            val libFolder: String?
            if (libZ3FilesUrl.toURI().scheme == "jar") {
                val tempDir = FileUtil.createTempDirectory("libs-").toFile()

                allLibraries.forEach { name ->
                    Z3Initializer::class.java
                        .classLoader
                        .getResourceAsStream("$z3DistFolder/$name$ext")
                        ?.use { input ->
                            File(tempDir, "$name$ext")
                                .outputStream()
                                .use { output -> input.copyTo(output) }
                        } ?: error("Can't find file: $name$ext")
                }

                libFolder = "$tempDir"
            } else {
                libFolder = File(libZ3FilesUrl.file).parent
            }

            allLibraries.forEach { System.load("$libFolder/$it$ext") }
        }

        init {
            initializeCallback
        }
    }
}