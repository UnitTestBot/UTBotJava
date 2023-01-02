val ideType: String by settings

val pythonIde: String by settings
val jsIde: String by settings
val includeRiderInBuild: String by settings
val goIde: String by settings

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.name == "rdgen") {
                useModule("com.jetbrains.rd:rd-gen:${requested.version}")
            }
        }
    }
}

rootProject.name = "utbot"

include("utbot-core")
include("utbot-framework")
include("utbot-framework-api")
include("utbot-intellij")
include("utbot-sample")
include("utbot-fuzzers")
include("utbot-fuzzing")
include("utbot-junit-contest")
include("utbot-analytics")
include("utbot-analytics-torch")

include("utbot-cli")

include("utbot-api")
include("utbot-instrumentation")
include("utbot-instrumentation-tests")

include("utbot-summary")
include("utbot-gradle")
include("utbot-maven")
include("utbot-summary-tests")
include("utbot-framework-test")
include("utbot-testing")
include("utbot-rd")
include("utbot-android-studio")

if (includeRiderInBuild.toBoolean()) {
    include("utbot-rider")
}

include("utbot-ui-commons")

if (pythonIde.split(",").contains(ideType)) {
    include("utbot-python")
    include("utbot-cli-python")
    include("utbot-intellij-python")
}

if (jsIde.split(",").contains(ideType)) {
    include("utbot-js")
    include("utbot-cli-js")
    include("utbot-intellij-js")
}

if (goIde.split(",").contains(ideType)) {
    include("utbot-go")
    include("utbot-cli-go")
    include("utbot-intellij-go")
}
