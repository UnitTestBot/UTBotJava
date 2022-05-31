package org.utbot.examples.samples

class ExampleClass {
    var x1 = 1
    val arr = BooleanArray(5)
    val arr2 = BooleanArray(10)

    fun bar(x: Int) {
        if (x > 1) {
            x1++
            x1++
        } else {
            x1--
            x1--
        }
    }

    fun kek2(x: Int) {
        arr[x] = true
    }

    fun foo(x: Int): Int {
        x1 = x xor 2

        var was = false

        for (i in 0 until x) {
            was = true
            var x2 = 0
            if (i > 5) {
                was = false
                x2 = 1
            }
            if (was && x2 == 0) {
                was = true
            }
        }

        // empty lines
        return if (was) x1 else x1 + 1
    }

    fun dependsOnField() {
        x1 = x1 xor 1
        if (x1 and 1 == 1) {
            x1 += 4
        } else {
            x1 += 2
        }
    }

    fun dependsOnFieldReturn(): Int {
        x1 = x1 xor 1
        if (x1 and 1 == 1) {
            x1 += 4
        } else {
            x1 += 2
        }
        return x1
    }

    fun emptyMethod() {
    }

    @Suppress("unused")
    fun use() = arr2.size
}