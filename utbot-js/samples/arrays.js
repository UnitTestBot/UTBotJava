///region Simple array test
function simpleArray(arr) {
    if (arr[0] === 5) {
        return 5
    }
    return 1
}
simpleArray([0, 2])
///endregion
///region Array of objects test
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
///endregion
///region Function returns array test
function returnsArray(num) {
    return [num]
}
returnsArray(5)
///endregion