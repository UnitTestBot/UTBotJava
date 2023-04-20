package org.utbot.data

enum class JDKVersion (val namePart: String, val number: Int) {

    JDK_1_8(namePart = "1.8", 8),
    JDK_11(namePart = "11", 11),
    JDK_17(namePart = "17", 17),
    JDK_19(namePart = "19", 19);

    override fun toString() = namePart
}