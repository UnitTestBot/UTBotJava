package analyzer;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextException;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

@SpringBootApplication
public class SpringAnalysisRunner {

    public static void main(String[] args) throws ClassNotFoundException, IOException, NoSuchFieldException, IllegalAccessException {
        //String arg0 = "D:\\Projects\\spring-starter-lesson-28\\build\\classes\\java\\main";
        //String arg1 = "com.dmdev.spring.config.ApplicationConfiguration";
        //String arg2 = "D:\\Projects\\spring-starter-lesson-28\\build\\resources\\main\\application.properties";

        ClassLoader classLoader = new URLClassLoader(new URL[]{Path.of(args[0]).toUri().toURL()});
        Class<?> userConfigurationClass = classLoader.loadClass(args[1]);

        ConfigurationManager configurationManager = new ConfigurationManager(classLoader, userConfigurationClass);
        PropertiesAnalyzer propertiesAnalyzer = new PropertiesAnalyzer(args[2]);

        configurationManager.patchPropertySourceAnnotation();

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
