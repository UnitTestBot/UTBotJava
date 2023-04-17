const {ObjectParameter,glob} = require("./toImport.js")

function test(obj) {
    return obj.performAction(5)
}

test(new ObjectParameter(glob))