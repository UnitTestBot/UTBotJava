package examples

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.utbot.framework.UtSettings

class CustomJavaDocTagsEnabler(private val enable: Boolean = true) : BeforeEachCallback, AfterEachCallback {
    private var previousValue = false

    override fun beforeEach(context: ExtensionContext?) {
        previousValue = UtSettings.useCustomJavaDocTags
        UtSettings.useCustomJavaDocTags = enable
    }

    override fun afterEach(context: ExtensionContext?) {
        UtSettings.useCustomJavaDocTags = previousValue
    }
}