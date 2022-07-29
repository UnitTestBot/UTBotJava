package org.utbot.framework.plugin.jcdb

import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.ClasspathSet

interface VirtualClassId : ClassId {
    override var classpath: ClasspathSet
}

interface VirtualClasspathSet : ClasspathSet {

    fun <CLASS_ID: VirtualClassId> bind(virtualClass: CLASS_ID): CLASS_ID
}