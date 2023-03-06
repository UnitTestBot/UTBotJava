package application

import analyzers.PropertiesAnalyzer
import analyzers.XmlConfigurationAnalyzer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextException
import utils.ConfigurationManager
import java.net.URLClassLoader
import java.nio.file.Path

@SpringBootApplication
open class SpringAnalysisRunner {

    fun main(args: Array<String>) {
        //val arg0 = "D:/Projects/spring-starter-lesson-28/build/classes/java/main";
        //val arg1 = "com.dmdev.spring.config.ApplicationConfiguration";
        //val arg2 = "D:/Projects/spring-starter-lesson-28/src/main/resources/application.properties";
        //val arg3 = "D:/Projects/spring-starter-lesson-28/src/main/resources/application.xml";

        val classLoader: ClassLoader = URLClassLoader(arrayOf(Path.of(args[0]).toUri().toURL()))
        val userConfigurationClass = classLoader.loadClass(args[1])

        val configurationManager = ConfigurationManager(classLoader, userConfigurationClass)
        val propertiesAnalyzer = PropertiesAnalyzer(args[2])
        val xmlConfigurationAnalyzer = XmlConfigurationAnalyzer(args[3])

        xmlConfigurationAnalyzer.fillFakeApplicationXml()

        configurationManager.patchPropertySourceAnnotation()
        configurationManager.patchImportResourceAnnotation()

        val app = SpringApplicationBuilder(SpringAnalysisRunner::class.java)
        app.sources(TestApplicationConfiguration::class.java, userConfigurationClass)
        for (prop in propertiesAnalyzer.readProperties()) {
            app.properties(prop)
        }

        try {
            app.build()
            app.run()
        } catch (e: ApplicationContextException) {
            println("Bean analysis finished successfully")
        }
    }
}

fun main(args: Array<String>) {
    val name = SpringAnalysisRunner()
    name.main(args)
}