// Sets in JavaScript are untyped, so only sets with basic value types are feasible to support
///region Simple Set test
function setTest(set, checkValue) {
    if (set.has(checkValue)) {
        return set
    }
    return set
}

let s = new Set()
s.add(5)
s.add(6)
setTest(s, 4)
///endregion
///region Function returns Set test
function returnsSet(num) {
    let temp = new Set()
    return temp.add(num)
}

returnsSet(5)
///endregion