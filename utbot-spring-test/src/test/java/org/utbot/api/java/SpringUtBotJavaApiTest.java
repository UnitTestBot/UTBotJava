package org.utbot.api.java;

import org.utbot.examples.spring.app.MyServiceImpl;
import org.junit.jupiter.api.Test;
import org.utbot.examples.spring.app.MyServiceUser;
import org.utbot.examples.spring.app.SpringExampleApp;
import org.utbot.examples.spring.config.pure.ExamplePureSpringConfig;
import org.utbot.external.api.UtBotJavaApi;
import org.utbot.external.api.UtBotSpringApi;
import org.utbot.framework.codegen.domain.ForceStaticMocking;
import org.utbot.framework.codegen.domain.Junit4;
import org.utbot.framework.codegen.domain.MockitoStaticMocking;
import org.utbot.framework.codegen.domain.ProjectType;
import org.utbot.framework.context.ApplicationContext;
import org.utbot.framework.context.spring.SpringApplicationContext;
import org.utbot.framework.plugin.api.*;
import org.utbot.framework.util.Snippet;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

import static org.utbot.framework.plugin.api.MockFramework.MOCKITO;
import static org.utbot.framework.util.TestUtilsKt.compileClassFile;

public class SpringUtBotJavaApiTest  extends AbstractUtBotJavaApiTest {

    /**
     * We are using the environment to check spring generation the only difference in the data supplied by the argument
     * @param applicationContext returns Spring settings to run the generation with
     */
    private void supplyConfigurationAndRunSpringTest(ApplicationContext applicationContext) {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(MyServiceImpl.class);
        String dependencyClassPath = getDependencyClassPath();

        Method getNameMethod = getMethodByName(
                MyServiceImpl.class,
                "getName"
        );

        Method useMyServiceMethod = getMethodByName(
                MyServiceUser.class,
                "useMyService"
        );

        List<UtMethodTestSet> myServiceUserTestSets = UtBotJavaApi.generateTestSetsForMethods(
                Collections.singletonList(useMyServiceMethod),
                MyServiceUser.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                60000L,
                applicationContext
        );

        String generationResult = UtBotJavaApi.generateTestCode(
                Collections.emptyList(),
                myServiceUserTestSets,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                MyServiceUser.class,
                ProjectType.Spring,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                MyServiceUser.class.getPackage().getName(),
                applicationContext
        );

        Snippet snippet = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet);
    }

    @Test
    public void testUnitTestWithoutSettings() {
        supplyConfigurationAndRunSpringTest(UtBotSpringApi.createSpringApplicationContext(
                        SpringSettings.AbsentSpringSettings,
                        SpringTestType.UNIT_TEST,
                        Collections.emptyList()));
    }

    @Test
    public void testUnitTestWithSettings() {
        SpringConfiguration configuration =
                UtBotSpringApi.createJavaSpringConfiguration(SpringExampleApp.class);

        SpringSettings springSettings =
                new SpringSettings.PresentSpringSettings(configuration, Arrays.asList("test1", "test2"));

        ApplicationContext applicationContext = UtBotSpringApi.createSpringApplicationContext(
                springSettings,
                SpringTestType.UNIT_TEST,
                Arrays.asList(getDependencyClassPath().split(File.pathSeparator))
        );

        supplyConfigurationAndRunSpringTest(applicationContext);
    }
}
