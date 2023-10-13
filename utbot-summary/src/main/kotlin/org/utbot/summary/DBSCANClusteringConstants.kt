package org.utbot.summary

import org.utbot.framework.plugin.api.util.IndentUtil

object DBSCANClusteringConstants {
    /**
     * Sets minimum number of successful execution
     * for applying the clustering algorithm.
     */
    internal const val MIN_NUMBER_OF_EXECUTIONS_FOR_CLUSTERING: Int = 4

    /**
     * DBSCAN hyperparameter.
     *
     * Sets minimum number of executions to form a cluster.
     */
    internal const val MIN_EXEC_DBSCAN: Int = 2

    /**
     * DBSCAN hyperparameter.
     *
     * Sets radius of search for algorithm.
     */
    internal const val RADIUS_DBSCAN: Float = 5.0f
}

object SummarySentenceConstants {
    const val SENTENCE_SEPARATION = ",\n"
    const val TAB = IndentUtil.TAB
    const val NEW_LINE = "\n"
    const val DOT_SYMBOL = '.'
    const val COMMA_SYMBOL = ','
    const val SEMI_COLON_SYMBOL = ';'
    const val CARRIAGE_RETURN = "\r"

    const val FROM_TO_NAMES_TRANSITION = "->"
    const val FROM_TO_NAMES_COLON = ":"
    const val AT_CODE = "@code"
    const val OPEN_BRACKET = "{"
    const val CLOSE_BRACKET = "}"

    const val JAVA_CLASS_DELIMITER = "$"
    const val JAVA_DOC_CLASS_DELIMITER = "."
}