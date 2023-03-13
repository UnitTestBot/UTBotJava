// Maps in JavaScript are untyped, so only maps with basic key/value types are feasible to support
function simpleMap(map, compareValue) {
    if (map.get("a") === compareValue) {
        return 5
    }
    return 1
}

const map1 = new Map()
map1.set("b", 3.0)
simpleMap(map1, 5)