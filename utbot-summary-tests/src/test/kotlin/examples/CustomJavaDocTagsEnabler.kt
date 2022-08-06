package examples

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.utbot.summary.UtSummarySettings

class CustomJavaDocTagsEnabler(private val enable: Boolean = true) : BeforeEachCallback, AfterEachCallback {
    private var previousValue = false

    override fun beforeEach(context: ExtensionContext?) {
        previousValue = UtSummarySettings.USE_CUSTOM_JAVADOC_TAGS
        UtSummarySettings.USE_CUSTOM_JAVADOC_TAGS = enable
    }

    override fun afterEach(context: ExtensionContext?) {
        UtSummarySettings.USE_CUSTOM_JAVADOC_TAGS = previousValue
    }
}