class StringExamples {

    isNotBlank(cs) {
        return cs.length !== 0
    }

    nullableStringBuffer(buffer, i) {
        if (i >= 0) {
            buffer += "Positive"
        } else {
            buffer += "Negative"
        }
        return buffer.toString()
    }

    length(cs) {
        return cs == null ? 0 : cs.length
    }
}