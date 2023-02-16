package analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextException;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;


@SpringBootApplication
public class SpringBeansAnalyzer {

    public static void main(String[] args) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = new URLClassLoader(new URL[]{Path.of(args[0]).toUri().toURL()});
        Class<?> userConfigurationClass = classLoader.loadClass(args[1]);

        SpringApplication app = new SpringApplicationBuilder(SpringBeansAnalyzer.class)
                .sources(TestApplicationConfiguration.class, userConfigurationClass)
                .build();

        try {
            app.run();
        } catch (ApplicationContextException e) {
            System.out.println("Bean analysis finished successfully");
        }
    }
}
