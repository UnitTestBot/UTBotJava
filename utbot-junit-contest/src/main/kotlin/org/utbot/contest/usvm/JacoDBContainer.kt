package org.utbot.contest.usvm

import kotlinx.coroutines.runBlocking
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcDatabase
import org.jacodb.impl.JcSettings
import org.jacodb.impl.jacodb
import java.io.File

// TODO usvm-sbft-refactoring: copied from `usvm/usvm-jvm/test`, extract this class back to USVM project
class JacoDBContainer(
    classpath: List<File>,
    builder: JcSettings.() -> Unit,
) {
    val db: JcDatabase
    val cp: JcClasspath

    init {
        val (db, cp) = runBlocking {
            val db = jacodb(builder)
            // TODO usvm-sbft: do we need `db.classpath(classpath)` or `db.classpathWithApproximations(classpath)` for contest?
            val cp = db.classpath(classpath)
            db to cp
        }
        this.db = db
        this.cp = cp
        runBlocking {
            db.awaitBackgroundJobs()
        }
    }

    companion object {
        private val keyToJacoDBContainer = HashMap<Any?, JacoDBContainer>()

        operator fun invoke(
            key: Any?,
            classpath: List<File>,
            builder: JcSettings.() -> Unit,
        ): JacoDBContainer =
            keyToJacoDBContainer.getOrPut(key) { JacoDBContainer(classpath, builder) }
    }
}
