package org.utbot.framework.plugin.jcdb

import org.utbot.jcdb.api.ByteCodeLocation
import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.ClasspathSet
import org.utbot.jcdb.api.JCDB
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

class DelegatingClasspathSet(private val cp: ClasspathSet) : VirtualClasspathSet {

    override val db: JCDB
        get() = cp.db
    override val locations: List<ByteCodeLocation>
        get() = cp.locations

    private val virtualClasses = ConcurrentHashMap<String, VirtualClassId>()

    override fun <CLASS_ID: VirtualClassId> bind(virtualClass: CLASS_ID): CLASS_ID {
        virtualClass.classpath = this
        return virtualClasses.getOrPut(virtualClass.name) {
            virtualClass
        } as CLASS_ID
    }

    override suspend fun findClassOrNull(name: String): ClassId? {
        return virtualClasses[name] ?: cp.findClassOrNull(name)
    }

    override suspend fun findSubClasses(name: String, allHierarchy: Boolean): List<ClassId> {
        return cp.findSubClasses(name, allHierarchy)
    }

    override suspend fun findSubClasses(classId: ClassId, allHierarchy: Boolean): List<ClassId> {
        return cp.findSubClasses(classId, allHierarchy)
    }

    override suspend fun <T : Serializable> query(key: String, term: String): List<T> {
        return cp.query(key, term)
    }

    override suspend fun <T : Serializable> query(key: String, location: ByteCodeLocation, term: String): List<T> {
        return cp.query(key, location, term)
    }

    override suspend fun refreshed(closeOld: Boolean): ClasspathSet {
        return DelegatingClasspathSet(cp.refreshed(closeOld))
    }

    override fun close() {
        cp.close()
    }
}

