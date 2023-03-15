package application

import analyzers.SpringApplicationAnalyzer
import utils.PathsUtils

/**
 * To run this app, arguments must be passed in the following way:
 * args[0] - classpath of current project
 * args[1] - fully qualified name of configuration class
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
    val arg2 = "/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application.properties;/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application-web.properties"
    val arg3 = "/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application.xml;/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application2.xml"
    */

    val springApplicationAnalyzer = SpringApplicationAnalyzer(
        applicationPath = args[0],
        configurationClassFqn = args[1],
        propertyFilesPaths = args[2].split(";").filter { it != PathsUtils.EMPTY_PATH },
        xmlConfigurationPaths = args[3].split(";").filter { it != PathsUtils.EMPTY_PATH },
    )

    springApplicationAnalyzer.analyze()
}