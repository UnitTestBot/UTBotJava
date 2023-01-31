class BitOperators {

    complement(x) {
        return (~x) === 1
    }

    xor(x, y) {
        return (x ^ y) === 0
    }

    and(x) {
        return (x & (x - 1)) === 0
    }

    Not(a, b) {
        let d = a && b
        let e = !a || b
        return d && e ? 100 : 200
    }

    shl(x) {
        return (x << 1) === 2
    }

    shlWithBigLongShift(shift) {
        if (shift < 40) {
            return 1
        }
        return (0x77777777 << shift) === 0x77777770 ? 2 : 3
    }
}