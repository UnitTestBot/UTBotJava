const {ObjectParameter} = require("./toImport.js")

function test(obj) {
    return obj.performAction(5)
}

test(new ObjectParameter(5))
