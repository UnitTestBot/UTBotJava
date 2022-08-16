package org.utbot.python

object Cleaner {
    private var clean: () -> Unit = {}

    fun addFunction(f: () -> Unit) {
        val oldClean = clean
        val newClean = {
            oldClean()
            f()
        }
        clean = newClean
    }

    fun restart() {
        clean = {}
    }

    fun doCleaning() {
        clean()
    }
}