package org.utbot.engine.taint

import soot.jimple.Stmt

data class TaintSourceData(
    val stmt: Stmt,
    val taintKinds: TaintKinds
)

data class TaintSinkData(
    val stmt: Stmt,
    val taintIndex: Int
)
