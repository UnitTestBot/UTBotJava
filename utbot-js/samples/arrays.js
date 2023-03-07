function simpleArray(arr) {
    if (arr[0] === 5) {
        return 5
    }
    return 1
}

simpleArray([0, 2])

class ObjectParameter {

    constructor(a) {
        this.first = a
    }

    performAction(value) {
        return 2 * value
    }
}

function arrayOfObjects(arr) {
    if (arr[0].first === 2) {
        return 1
    }
    return 10
}

let arr = []
arr[0] = new ObjectParameter(10)
arrayOfObjects(arr)