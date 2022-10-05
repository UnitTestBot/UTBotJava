class ObjectParameter {

    constructor(a) {
        this.first = a
    }

    performAction(value) {
        return 2 * value
    }
}

function functionToTest(obj, v) {
    return obj.performAction(v)
}