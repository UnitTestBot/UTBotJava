package org.utbot.quickcheck.generator

/**
 * Raised if a problem arises when attempting to configure a generator with
 * annotations from a property parameter.
 *
 * @see Generator.configure
 */
class GeneratorConfigurationException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable?) : super(message, cause)

    companion object {
        private const val serialVersionUID = 1L
    }
}