function functionToTest(a) {
    if (a === true) {
        throw Error("MyCustomError")
    } else if (a === 1) {
        while (true) {
        }
    } else {
        return -1
    }
}
