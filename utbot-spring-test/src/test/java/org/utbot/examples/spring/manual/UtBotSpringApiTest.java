package org.utbot.examples.spring.manual;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.utbot.common.FileUtil;
import org.utbot.common.JarUtils;
import org.utbot.examples.spring.config.boot.ExampleSpringBootConfig;
import org.utbot.examples.spring.config.pure.ExamplePureSpringConfig;
import org.utbot.external.api.UtBotSpringApi;
import org.utbot.framework.context.spring.SpringApplicationContext;
import org.utbot.framework.plugin.api.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class UtBotSpringApiTest {
    private static final List<String> DEFAULT_PROFILES = Collections.singletonList("default");

    @Test
    public void testOnAbsentSpringSettings() {
        SpringApplicationContext springApplicationContext =
                UtBotSpringApi.createSpringApplicationContext(
                        SpringSettings.AbsentSpringSettings,
                        SpringTestType.UNIT_TEST,
                        new ArrayList<>()
                );

        assertEquals(new ArrayList<>(), springApplicationContext.getBeanDefinitions());
    }

    @Test
    public void testOnSpringBootConfig() {
        List<BeanDefinitionData> actual = getBeansFromSampleProject(
                UtBotSpringApi.createJavaSpringConfiguration(ExampleSpringBootConfig.class),
                DEFAULT_PROFILES
        );

        List<BeanDefinitionData> expected = new ArrayList<>();
        expected.add(new BeanDefinitionData(
                "exampleSpringBootConfig",
                "org.utbot.examples.spring.config.boot.ExampleSpringBootConfig",
                null
        ));
        expected.add(new BeanDefinitionData(
                "exampleSpringBootService",
                "org.utbot.examples.spring.config.boot.ExampleSpringBootService",
                null
        ));

        assertEquals(expected, actual);
    }

    @Test
    public void testOnPureSpringConfig() {
        List<BeanDefinitionData> actual = getBeansFromSampleProject(
                UtBotSpringApi.createJavaSpringConfiguration(ExamplePureSpringConfig.class),
                DEFAULT_PROFILES
        );

        List<BeanDefinitionData> expected = new ArrayList<>();
        expected.add(new BeanDefinitionData("examplePureSpringConfig", "org.utbot.examples.spring.config.pure.ExamplePureSpringConfig", null));
        expected.add(new BeanDefinitionData(
                "exampleService0",
                "org.utbot.examples.spring.config.pure.ExamplePureSpringService",
                new BeanAdditionalData(
                        "exampleService",
                        Collections.emptyList(),
                        "org.utbot.examples.spring.config.pure.ExamplePureSpringConfig"
                )
        ));

        assertEquals(expected, actual);
    }

    @Test
    public void testOnPureSpringConfigWithProfiles() {
        List<String> profiles = new ArrayList<>();
        profiles.add("test1");
        profiles.add("test3");

        List<BeanDefinitionData> actual = getBeansFromSampleProject(
                UtBotSpringApi.createJavaSpringConfiguration(ExamplePureSpringConfig.class),
                profiles
        );

        List<BeanDefinitionData> expected = new ArrayList<>();
        expected.add(new BeanDefinitionData("examplePureSpringConfig", "org.utbot.examples.spring.config.pure.ExamplePureSpringConfig", null));
        expected.add(new BeanDefinitionData(
                "exampleService0",
                "org.utbot.examples.spring.config.pure.ExamplePureSpringService",
                new BeanAdditionalData(
                        "exampleService",
                        Collections.emptyList(),
                        "org.utbot.examples.spring.config.pure.ExamplePureSpringConfig"
                )
        ));
        expected.add(new BeanDefinitionData(
                "exampleServiceTest1",
                "org.utbot.examples.spring.config.pure.ExamplePureSpringService",
                new BeanAdditionalData(
                        "exampleServiceTest1",
                        Collections.emptyList(),
                        "org.utbot.examples.spring.config.pure.ExamplePureSpringConfig"
                )
        ));
        expected.add(new BeanDefinitionData(
                "exampleServiceTest3",
                "org.utbot.examples.spring.config.pure.ExamplePureSpringService",
                new BeanAdditionalData(
                        "exampleServiceTest3",
                        Collections.emptyList(),
                        "org.utbot.examples.spring.config.pure.ExamplePureSpringConfig"
                )
        ));

        assertEquals(expected, actual);
    }

    @Test
    public void testOnXmlConfig() throws IOException {
        URL configUrl = Objects.requireNonNull(getClass().getClassLoader().getResource("xml-spring-config.xml"));
        File configDir = FileUtil.INSTANCE.createTempDirectory("xml-config").toFile();
        File configFile = new File(configDir, "xml-spring-config.xml");
        FileUtils.copyURLToFile(configUrl, configFile);
        List<String> additionalClasspath = Collections.singletonList(configDir.getAbsolutePath());

        List<BeanDefinitionData> actual = getBeansFromSampleProject(
                UtBotSpringApi.createXmlSpringConfiguration(configFile),
                additionalClasspath
        );

        List<BeanDefinitionData> expected = new ArrayList<>();
        expected.add(new BeanDefinitionData(
                "xmlService",
                "org.utbot.examples.spring.config.xml.ExampleXmlService",
                null
        ));

        assertEquals(expected, actual);
    }

    static List<BeanDefinitionData> getBeansFromSampleProject(
            SpringConfiguration configuration,
            List<String> profiles
    ) {
        return getBeansFromSampleProject(configuration, profiles, new ArrayList<>());
    }

    static List<BeanDefinitionData> getBeansFromSampleProject(
            SpringConfiguration configuration,
            List<String> profiles,
            List<String> additionalClasspath
    ) {
        SpringSettings springSettings = new SpringSettings.PresentSpringSettings(configuration, profiles);
        SpringTestType springTestType = SpringTestType.UNIT_TEST;
        List<String> classpath = new ArrayList<>(additionalClasspath);
        classpath.add(
                JarUtils.INSTANCE.extractJarFileFromResources(
                        "utbot-spring-sample-shadow.jar",
                        "lib/utbot-spring-sample-shadow.jar",
                        "spring-sample"
                ).getAbsolutePath()
        );
        SpringApplicationContext springApplicationContext =
                UtBotSpringApi.createSpringApplicationContext(springSettings, springTestType, classpath);
        return springApplicationContext.getBeanDefinitions().stream()
                .filter(beanDef -> beanDef.getBeanTypeName().startsWith("org.utbot.examples"))
                .collect(Collectors.toList());
    }
}
