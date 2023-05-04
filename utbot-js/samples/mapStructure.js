// Maps in JavaScript are untyped, so only maps with basic key/value types are feasible to support
///region Simple Map test
function simpleMap(map, compareValue) {
    if (map.get("a") === compareValue) {
        return 5
    }
    return 1
}

const map1 = new Map()
map1.set("b", 3.0)
simpleMap(map1, 5)
///endregion
///region Function returns Map test
function returnsMap(name, value) {
    let temp = new Map()
    return temp.set(name, value)
}

returnsMap("a", 5)
///endregion
