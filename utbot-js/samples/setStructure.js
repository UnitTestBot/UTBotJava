// Sets in JavaScript are untyped, so only sets with basic value types are feasible to support
function setTest(set, checkValue) {
    if (set.has(checkValue)) {
        return 5
    }
    return 1
}

let s = new Set()
s.add(5)
s.add(6)
setTest(s, 4)