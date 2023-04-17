const {Some} = require("./temp.js")

class ObjectParameter {

    constructor(some) {
        this.first = some.b
    }

    performAction(value) {
        return 2 * value
    }
}
// Using global variable to "hide" actual class (Some) from the chainImports.js file
let glob = new ObjectParameter(new Some(5))

exports.glob = glob
exports.ObjectParameter = ObjectParameter
