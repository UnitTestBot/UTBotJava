function customError(a) {
    if (a > 5) {
        throw Error("MyCustomError")
    } else {
        return 10
    }
}

function goodBoy(a) {
    switch (a) {
        case 5:
            return 5
        case 10:
            return 10
        default:
            return 0
    }
}