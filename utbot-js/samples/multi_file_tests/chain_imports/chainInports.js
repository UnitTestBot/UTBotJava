const {glob} = require("./toImport.js")

function test(obj) {
    return obj.performAction(5)
}

test(glob)
