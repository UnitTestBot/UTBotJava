class Object {

    constructor(a) {
        this.first = a
    }

    static functionToTest(value) {
        if (value > 1024 && value < 1026) {
            return 2 * value
        }
        return value
    }
}
