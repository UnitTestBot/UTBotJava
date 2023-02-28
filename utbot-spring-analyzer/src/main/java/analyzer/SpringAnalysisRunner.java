package analyzer;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextException;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

@SpringBootApplication
public class SpringAnalysisRunner {

    public static void main(String[] args) throws Exception {
        //String arg0 = "/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/build/classes/java/main";
        //String arg1 = "com.dmdev.spring.config.ApplicationConfiguration";
        //String arg2 = "/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application.properties";
        //String arg3 = "/Users/kirillshishin/IdeaProjects/spring-starter-lesson-28/src/main/resources/application.xml";

        ClassLoader classLoader = new URLClassLoader(new URL[]{Path.of(args[0]).toUri().toURL()});
        Class<?> userConfigurationClass = classLoader.loadClass(args[1]);

        ConfigurationManager configurationManager = new ConfigurationManager(classLoader, userConfigurationClass);
        PropertiesAnalyzer propertiesAnalyzer = new PropertiesAnalyzer(args[2]);
        XmlBeanAnalyzer xmlBeanAnalyzer = new XmlBeanAnalyzer(args[3]);

        xmlBeanAnalyzer.rewriteXmlFile();
        // call action to update project tree

        configurationManager.patchPropertySourceAnnotation();
        configurationManager.patchImportResourceAnnotation();

        SpringApplicationBuilder app = new SpringApplicationBuilder(SpringAnalysisRunner.class)
                .sources(TestApplicationConfiguration.class, userConfigurationClass);

        for (String prop : propertiesAnalyzer.readProperties()) {
            app.properties(prop);
        }

        try {
            app.build();
            app.run();
        } catch (ApplicationContextException e) {
            System.out.println("Bean analysis finished successfully");
        }
    }

}
