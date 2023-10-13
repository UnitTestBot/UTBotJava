package org.utbot.modifications.util

import soot.SootClass
import soot.SootMethod
import soot.jimple.JimpleBody

/**
 * Retrieves Jimple body of SootMethod.
 */
fun retrieveJimpleBody(sootMethod: SootMethod): JimpleBody? =
    if (sootMethod.canRetrieveBody()) sootMethod.jimpleBody() else null

private fun SootMethod.canRetrieveBody() =
    runCatching { retrieveActiveBody() }.isSuccess

private fun SootMethod.jimpleBody(): JimpleBody {
    declaringClass.adjustLevel(SootClass.BODIES)
    return retrieveActiveBody() as JimpleBody
}

private fun SootClass.adjustLevel(level: Int) {
    if (resolvingLevel() < level) {
        setResolvingLevel(level)
    }
}
