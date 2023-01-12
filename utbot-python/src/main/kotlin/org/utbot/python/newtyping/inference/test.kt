package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.pythonTypeRepresentation

fun main() {
    TypeInferenceProcessor(
        "python3",
        directoriesForSysPath = setOf("/home/tochilinak/Documents/projects/utbot/django-cms"),
        "/home/tochilinak/Documents/projects/utbot/django-cms/cms/api.py",
        moduleOfSourceFile = "cms.api",
        "_verify_apphook"
    ).inferTypes(cancel = { false }).forEach {
        println(it.pythonTypeRepresentation())
    }
}