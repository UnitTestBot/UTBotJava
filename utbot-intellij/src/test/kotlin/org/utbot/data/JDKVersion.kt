package org.utbot.data

enum class JDKVersion (val namePart: String, val number: Int, val supported: Boolean) {

    JDK_1_8(namePart = "1.8", 8, true),
    JDK_11(namePart = "11", 11, true),
    JDK_17(namePart = "17", 17, true),
    JDK_19(namePart = "19", 19, false);
    override fun toString() = namePart
}