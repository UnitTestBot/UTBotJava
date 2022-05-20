package org.utbot.framework.modifications

import org.utbot.engine.canRetrieveBody
import org.utbot.engine.jimpleBody
import soot.SootMethod
import soot.jimple.JimpleBody

/**
 * Retrieves Jimple body of SootMethod.
 */
fun retrieveJimpleBody(sootMethod: SootMethod): JimpleBody? =
    if (sootMethod.canRetrieveBody()) sootMethod.jimpleBody() else null