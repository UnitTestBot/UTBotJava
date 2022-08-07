package org.utbot.summary

object UtSummarySettings {
    /**
     * If True test comments will be generated
     */
    var GENERATE_COMMENTS = true

    /**
     * If True cluster comments will be generated
     */
    var GENERATE_CLUSTER_COMMENTS = true

    /**
     * If True names for tests will be generated
     */
    var GENERATE_NAMES = true

    /**
     * If True display names for tests will be generated
     */
    var GENERATE_DISPLAY_NAMES = true

    /**
     *  generate display name in from -> to style
     */
    var GENERATE_DISPLAYNAME_FROM_TO_STYLE = true

    /**
     * If True mutation descriptions for tests will be generated
     * TODO: implement
     */
    var GENERATE_MUTATION_DESCRIPTIONS = true

    /**
     * Sets minimum number of successful execution
     * for applying the clustering algorithm
     */
    const val MIN_NUMBER_OF_EXECUTIONS_FOR_CLUSTERING: Int = 4

    /**
     * DBSCAN hyperparameter
     * Sets minimum number of executions to form a cluster
     */
    var MIN_EXEC_DBSCAN: Int = 2

    /**
     * DBSCAN hyperparameter
     * Sets radius of search for algorithm
     */
    var RADIUS_DBSCAN: Float = 5.0f
}

object SummarySentenceConstants {
    const val SENTENCE_SEPARATION = ",\n"
    const val TAB = "    "
    const val NEW_LINE = "\n"
    const val DOT_SYMBOL = '.'
    const val COMMA_SYMBOL = ','
    const val SEMI_COLON_SYMBOL = ';'
    const val CARRIAGE_RETURN = "\r"

    const val FROM_TO_NAMES_TRANSITION = "->"
    const val AT_CODE = "@code"
    const val OPEN_BRACKET = "{"
    const val CLOSE_BRACKET = "}"
}