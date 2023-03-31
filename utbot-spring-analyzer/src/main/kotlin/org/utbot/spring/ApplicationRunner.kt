package org.utbot.spring

import org.utbot.spring.analyzers.SpringApplicationAnalyzer
import org.utbot.spring.data.ApplicationData
import org.utbot.spring.utils.PathsUtils
import java.io.File

/**
 * To run this app, arguments must be passed in the following way:
 * args[0] - classpath of current project
 * args[1] - fully qualified name of configuration class or main .xml configuration file path
 * args[2] - `.properties` file paths, separated via `;`, empty string if no files exist
 * args[3] - `.xml` configuration file paths
 *
 * Several items in one arg are separated via `;`.
 * If there are no files, empty string should be passed.
 */
fun main(args: Array<String>) {

    /* FOR EXAMPLE
    val arg0 = "/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/build/classes/java/main"

    val arg1 = "com.dmdev.spring.config.ApplicationConfiguration"
    or
    val arg1 = "/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application.xml"

    val arg2 = "/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application.properties;/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application-web.properties"
    val arg3 = "/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application.xml;/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application2.xml"
    */

    val applicationData =
        ApplicationData(
            applicationUrlArray = args[0].split(';').filter { it != PathsUtils.EMPTY_PATH }
                .map { File(it).toURI().toURL() }.toTypedArray(),
            configurationFile = args[1],
            propertyFilesPaths = args[2].split(";").filter { it != PathsUtils.EMPTY_PATH },
            xmlConfigurationPaths = args[3].split(";").filter { it != PathsUtils.EMPTY_PATH },
        )

    SpringApplicationAnalyzer(applicationData).analyze()
}