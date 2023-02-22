package org.utbot.taint.parser.yaml

/**
 * String constants in YAML configuration file.
 */
object Constants {
    const val KEY_SOURCES = "sources"
    const val KEY_PASSES = "passes"
    const val KEY_CLEANERS = "cleaners"
    const val KEY_SINKS = "sinks"

    const val KEY_ADD_TO = "add-to"
    const val KEY_GET_FROM = "get-from"
    const val KEY_REMOVE_FROM = "remove-from"
    const val KEY_CHECK = "check"
    const val KEY_MARKS = "marks"

    const val KEY_SIGNATURE = "signature"
    const val KEY_CONDITIONS = "conditions"

    const val KEY_CONDITION_NOT = "not"

    const val KEY_THIS = "this"
    const val KEY_RETURN = "return"
    const val KEY_ARG = "arg"

    const val ARGUMENT_TYPE_PREFIX = "<"
    const val ARGUMENT_TYPE_SUFFIX = ">"
    const val ARGUMENT_TYPE_UNDERSCORE = "_"
}
