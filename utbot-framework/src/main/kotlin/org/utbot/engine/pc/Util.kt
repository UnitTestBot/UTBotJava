package org.utbot.engine.pc

import com.microsoft.z3.Expr
import com.microsoft.z3.Sort

@Suppress("UNCHECKED_CAST")
fun <T : Sort, R : Sort> Expr<T>.cast(): Expr<R> = this as Expr<R>
