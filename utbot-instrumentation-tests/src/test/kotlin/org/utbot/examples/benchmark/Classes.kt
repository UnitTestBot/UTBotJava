package org.utbot.examples.benchmark

import javafx.util.Pair

class Repeater(var sep: String) {
    /*    public DifferentClass0() {
         this.sep = "-";
     }*/
    fun repeat(str: String?, times: Int): String {
        return concat(sep, str, times)
    }

    fun concat(x: String?, y: String?, times: Int): String {
        val sb = StringBuilder()
        for (i in 0 until times) {
            sb.append(y)
        }
        sb.append(x)
        return sb.toString()
    }
}

class Unzipper {
    var dc0 = Repeater("-")
    fun unzip(chars: Array<Pair<Int?, Char>>): String {
        val sb = java.lang.StringBuilder()
        for (pr in chars) {
            sb.append(dc0.repeat(pr.value.toString(), pr.key!!))
        }
        return sb.toString()
    }
}