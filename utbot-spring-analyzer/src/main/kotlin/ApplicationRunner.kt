package application

import analyzers.SpringApplicationAnalyzer

fun main(args: Array<String>) {
    /*
    val arg0 = "D:/Projects/spring-starter-lesson-28/build/classes/java/main";
    val arg1 = "com.dmdev.spring.config.ApplicationConfiguration";
    val arg2 = "D:/Projects/spring-starter-lesson-28/src/main/resources/application.properties";
    val arg3 = "D:/Projects/spring-starter-lesson-28/src/main/resources/application.xml";
    val springApplicationAnalyzer = SpringApplicationAnalyzer(arg0, arg1, arg2, arg3)
    */

    val springApplicationAnalyzer = SpringApplicationAnalyzer(
        applicationPath = args[0],
        configurationClassFqn = args[1],
        propertyFilePath = args[2],
        xmlConfigurationPath = args[3],
    )

    springApplicationAnalyzer.analyze()
}