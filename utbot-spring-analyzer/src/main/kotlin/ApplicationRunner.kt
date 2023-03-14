package application

import analyzers.SpringApplicationAnalyzer

fun main(args: Array<String>) {
    /*
        arg2 contains .properties files, arg3 contains .xml files.
        If there are several files, they are transmitted via ";".
        If there are no files, then an empty string "" is passed
    */

    /* FOR EXAMPLE
    val arg0 = "/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/build/classes/java/main"
    val arg1 = "com.dmdev.spring.config.ApplicationConfiguration"
    val arg2 = "/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application.properties;/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application-web.properties"
    val arg3 = "/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application.xml;/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application2.xml"
    */

    val springApplicationAnalyzer = SpringApplicationAnalyzer(
        applicationPath = args[0],
        configurationClassFqn = args[1],
        propertyFilesPaths = args[2].split(";"),
        xmlConfigurationPaths = args[3].split(";"),
    )

    springApplicationAnalyzer.analyze()
}