class Recursion {
    factorial(n) {
        if (n < 0)
            return -1
        if (n === 0)
            return 1
        return n * this.factorial(n - 1)
    }

    fib(n) {
        if (n < 0 || n > 25)
            return -1
        if (n === 0)
            return 0
        if (n === 1)
            return 1
        return this.fib(n - 1) + this.fib(n - 2)
    }
}
