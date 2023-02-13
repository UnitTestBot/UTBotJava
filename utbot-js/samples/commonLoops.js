class Loops {

    whileLoop(value) {
        let i = 0
        let sum = 0
        while (i < value) {
            sum += i
            i += 1
        }
        return sum
    }

    loopInsideLoop(x) {
        for (let i = x - 5; i < x; i++) {
            if (i < 0) {
                return 2
            } else {
                for (let j = i; j < x + i; j++) {
                    if (j === 7) {
                        return 1
                    }
                }
            }
        }
        return -1
    }
}
