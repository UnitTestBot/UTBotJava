package org.utbot.examples.manual;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.utbot.common.PathUtil;
import org.utbot.examples.assemble.DirectAccess;
import org.utbot.examples.assemble.PrimitiveFields;
import org.utbot.examples.assemble.arrays.ArrayOfComplexArrays;
import org.utbot.examples.assemble.arrays.ArrayOfPrimitiveArrays;
import org.utbot.examples.assemble.arrays.AssignedArray;
import org.utbot.examples.assemble.arrays.ComplexArray;
import org.utbot.examples.manual.examples.*;
import org.utbot.examples.manual.examples.customer.B;
import org.utbot.examples.manual.examples.customer.C;
import org.utbot.examples.manual.examples.customer.Demo9;
import org.utbot.external.api.TestMethodInfo;
import org.utbot.external.api.UtBotJavaApi;
import org.utbot.external.api.UtModelFactory;
import org.utbot.framework.codegen.ForceStaticMocking;
import org.utbot.framework.codegen.Junit4;
import org.utbot.framework.codegen.MockitoStaticMocking;
import org.utbot.framework.plugin.api.*;
import org.utbot.framework.plugin.api.util.UtContext;
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider;
import org.utbot.framework.util.Snippet;
import org.utbot.framework.util.SootUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.framework.plugin.api.MockFramework.MOCKITO;
import static org.utbot.framework.plugin.api.util.IdUtilKt.getIntArrayClassId;
import static org.utbot.framework.util.TestUtilsKt.compileClassAndGetClassPath;
import static org.utbot.framework.util.TestUtilsKt.compileClassFile;

class PredefinedGeneratorParameters {

    static String destinationClassName = "GeneratedTest";

    static Method getMethodByName(Class<?> clazz, String name, Class<?>... parameters) {
        try {
            return clazz.getDeclaredMethod(name, parameters);
        } catch (NoSuchMethodException ignored) {
            Assertions.fail();
        }
        throw new RuntimeException();
    }
}

public class UtBotJavaApiTest {
    private AutoCloseable context;
    private UtModelFactory modelFactory;

    @BeforeEach
    public void setUp() {
        context = UtContext.Companion.setUtContext(new UtContext(PrimitiveFields.class.getClassLoader()));
        modelFactory = new UtModelFactory();
    }

    @AfterEach
    public void tearDown() {
        try {
            context.close();
            modelFactory = null;
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    public void testMultiMethodClass() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(MultiMethodExample.class);
        String dependencyClassPath = getDependencyClassPath();

        UtCompositeModel classUnderTestModel = modelFactory.produceCompositeModel(
                classIdForType(MultiMethodExample.class)
        );

        EnvironmentModels initialState = new EnvironmentModels(
                classUnderTestModel,
                Collections.emptyList(),
                Collections.emptyMap()
        );

        EnvironmentModels thirdMethodState = new EnvironmentModels(
                classUnderTestModel,
                Collections.singletonList(new UtPrimitiveModel("some")),
                Collections.emptyMap()
        );


        Method firstMethodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                MultiMethodExample.class,
                "firstMethod"
        );

        Method secondMethodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                MultiMethodExample.class,
                "secondMethod"
        );

        Method thirdMethodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                MultiMethodExample.class,
                "thirdMethod",
                String.class
        );

        TestMethodInfo firstTestMethodInfo = new TestMethodInfo(
                firstMethodUnderTest,
                initialState);
        TestMethodInfo secondTestMethodInfo = new TestMethodInfo(
                secondMethodUnderTest,
                initialState);
        TestMethodInfo thirdTestMethodInfo = new TestMethodInfo(
                thirdMethodUnderTest,
                thirdMethodState);

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSets(
                Arrays.asList(firstTestMethodInfo, secondTestMethodInfo, thirdTestMethodInfo),
                MultiMethodExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResult = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                MultiMethodExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                MultiMethodExample.class.getPackage().getName()
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet1);
        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generate(
                Arrays.asList(firstTestMethodInfo, secondTestMethodInfo, thirdTestMethodInfo),
                Collections.emptyList(),
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                MultiMethodExample.class
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2);
    }

    @Test
    public void testCustomPackage() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(DirectAccess.class);
        String dependencyClassPath = getDependencyClassPath();

        HashMap<String, UtModel> fields = new HashMap<>();
        fields.put("stringClass", modelFactory.produceClassRefModel(String.class));

        UtCompositeModel classUnderTestModel = modelFactory.produceCompositeModel(
                classIdForType(ClassRefExample.class),
                fields
        );

        UtClassRefModel classRefModel = modelFactory.produceClassRefModel(Class.class);

        EnvironmentModels initialState = new EnvironmentModels(
                classUnderTestModel,
                Collections.singletonList(classRefModel),
                Collections.emptyMap()
        );

        Method methodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                ClassRefExample.class,
                "assertInstance",
                Class.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSets(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState)
                ),
                ClassRefExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResult = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                ClassRefExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                "some.custom.name"
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet1);

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generate(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState)
                ),
                Collections.emptyList(),
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                ClassRefExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                "some.custom.name"
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2);
    }

    @Test
    public void testOnObjectWithAssignedArrayField() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(DirectAccess.class);
        String dependencyClassPath = getDependencyClassPath();

        ClassId classIdAssignedArray = classIdForType(AssignedArray.class);

        HashMap<Integer, UtModel> values = new HashMap<>();
        values.put(0, new UtPrimitiveModel(1));
        values.put(1, new UtPrimitiveModel(2));

        UtArrayModel utArrayModel = modelFactory.produceArrayModel(
                getIntArrayClassId(),
                10,
                new UtPrimitiveModel(0),
                values
        );

        UtCompositeModel compositeModel = modelFactory.produceCompositeModel(
                classIdAssignedArray,
                Collections.singletonMap("array", utArrayModel)
        );

        UtCompositeModel classUnderTestModel = modelFactory.produceCompositeModel(
                classIdForType(AssignedArrayExample.class)
        );

        EnvironmentModels initialState = new EnvironmentModels(
                classUnderTestModel,
                Collections.singletonList(compositeModel),
                Collections.emptyMap()
        );


        Method methodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                AssignedArrayExample.class,
                "foo",
                AssignedArray.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSets(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState)
                ),
                AssignedArrayExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResult = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                AssignedArrayExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet1);

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generate(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState)
                ),
                Collections.emptyList(),
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                AssignedArrayExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2);
    }

    @Test
    public void testClassRef() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(DirectAccess.class);
        String dependencyClassPath = getDependencyClassPath();

        HashMap<String, UtModel> fields = new HashMap<>();
        fields.put("stringClass", modelFactory.produceClassRefModel(String.class));

        UtCompositeModel classUnderTestModel = modelFactory.produceCompositeModel(
                classIdForType(ClassRefExample.class),
                fields
        );

        UtClassRefModel classRefModel = modelFactory.produceClassRefModel(Class.class);

        EnvironmentModels initialState = new EnvironmentModels(
                classUnderTestModel,
                Collections.singletonList(classRefModel),
                Collections.emptyMap()
        );

        Method methodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                ClassRefExample.class,
                "assertInstance",
                Class.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSets(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState)
                ),
                ClassRefExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResult = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                ClassRefExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );


        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet1);

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generate(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState)
                ),
                Collections.emptyList(),
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                ClassRefExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2);
    }

    @Test
    public void testObjectWithPublicFields() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(DirectAccess.class);
        String dependencyClassPath = getDependencyClassPath();

        ClassId testClassId = classIdForType(DirectAccess.class);
        ClassId innerClassId = classIdForType(PrimitiveFields.class);

        HashMap<String, UtModel> primitiveFields = new HashMap<>();
        primitiveFields.put("a", new UtPrimitiveModel(2));
        primitiveFields.put("b", new UtPrimitiveModel(4));

        HashMap<String, UtModel> fields = new HashMap<>();
        fields.put("a", new UtPrimitiveModel(2));
        fields.put("b", new UtPrimitiveModel(4));
        fields.put("s",
                modelFactory.produceCompositeModel(
                        innerClassId,
                        primitiveFields,
                        Collections.emptyMap()
                )
        );

        UtCompositeModel compositeModel = modelFactory.produceCompositeModel(
                testClassId,
                fields,
                Collections.emptyMap()
        );

        // This class does not contain any fields. Using overloads
        UtCompositeModel classUnderTestModel = modelFactory.produceCompositeModel(
                classIdForType(DirectAccessExample.class)
        );

        EnvironmentModels initialState = new EnvironmentModels(
                classUnderTestModel,
                Collections.singletonList(compositeModel),
                Collections.emptyMap()
        );


        Method methodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                DirectAccessExample.class,
                "foo",
                DirectAccess.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSets(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState)
                ),
                DirectAccessExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResult = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                DirectAccessExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet1);

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generate(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState)
                ),
                Collections.emptyList(),
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                DirectAccessExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2);
    }

    @Test
    public void testObjectWithPublicFieldsWithAssembleModel() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(DirectAccess.class);
        String dependencyClassPath = getDependencyClassPath();

        ClassId testClassId = classIdForType(DirectAccess.class);
        ClassId innerClassId = classIdForType(PrimitiveFields.class);

        HashMap<String, UtModel> primitiveFields = new HashMap<>();
        primitiveFields.put("a", new UtPrimitiveModel(2));
        primitiveFields.put("b", new UtPrimitiveModel(4));

        HashMap<String, UtModel> fields = new HashMap<>();
        fields.put("a", new UtPrimitiveModel(2));
        fields.put("b", new UtPrimitiveModel(4));
        fields.put("s",
                modelFactory.produceCompositeModel(
                        innerClassId,
                        primitiveFields,
                        Collections.emptyMap()
                )
        );

        UtCompositeModel compositeModel = modelFactory.produceCompositeModel(
                testClassId,
                fields,
                Collections.emptyMap()
        );

        // This class does not contain any fields. Using overloads
        UtCompositeModel classUnderTestModel = modelFactory.produceCompositeModel(
                classIdForType(DirectAccessExample.class)
        );

        EnvironmentModels initialState = new EnvironmentModels(
                classUnderTestModel,
                Collections.singletonList(compositeModel),
                Collections.emptyMap()
        );

        Method methodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                DirectAccessExample.class,
                "foo",
                DirectAccess.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSets(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState)
                ),
                DirectAccessExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResult = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                DirectAccessExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet1);
    }


    @Test
    public void testOnObjectWithArrayOfPrimitiveArrays() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(ArrayOfPrimitiveArraysExample.class);
        String dependencyClassPath = getDependencyClassPath();

        ClassId cidPrimitiveArrays = getIntArrayClassId();
        ClassId cidPrimitiveArraysOuter = new ClassId("[[I", cidPrimitiveArrays);
        ClassId classIdArrayOfPrimitiveArraysClass = classIdForType(ArrayOfPrimitiveArrays.class);
        ClassId cidArrayOfPrimitiveArraysTest = classIdForType(ArrayOfPrimitiveArraysExample.class);

        HashMap<Integer, UtModel> arrayParameters = new HashMap<>();
        arrayParameters.put(0, new UtPrimitiveModel(88));
        arrayParameters.put(1, new UtPrimitiveModel(42));

        UtArrayModel innerArrayOfPrimitiveArrayModel = modelFactory.produceArrayModel(
                cidPrimitiveArrays,
                2,
                new UtPrimitiveModel(0),
                arrayParameters
        );

        HashMap<Integer, UtModel> enclosingArrayParameters = new HashMap<>();
        enclosingArrayParameters.put(0, innerArrayOfPrimitiveArrayModel);
        enclosingArrayParameters.put(1, innerArrayOfPrimitiveArrayModel);

        UtArrayModel enclosingArrayOfPrimitiveArrayModel = modelFactory.produceArrayModel(
                cidPrimitiveArraysOuter,
                1,
                new UtNullModel(getIntArrayClassId()),
                enclosingArrayParameters
        );

        UtCompositeModel cmArrayOfPrimitiveArrays = modelFactory.produceCompositeModel(
                classIdArrayOfPrimitiveArraysClass,
                Collections.singletonMap("array", enclosingArrayOfPrimitiveArrayModel)
        );

        UtCompositeModel testClassCompositeModel = modelFactory.produceCompositeModel(
                cidArrayOfPrimitiveArraysTest
        );

        EnvironmentModels initialState = new EnvironmentModels(
                testClassCompositeModel,
                Collections.singletonList(cmArrayOfPrimitiveArrays),
                Collections.emptyMap()
        );

        Method methodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                ArrayOfPrimitiveArraysExample.class,
                "assign10",
                ArrayOfPrimitiveArrays.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSets(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState)
                ),
                ArrayOfPrimitiveArraysExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResult = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                ArrayOfPrimitiveArraysExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet);

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generate(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState)
                ),
                Collections.emptyList(),
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                ArrayOfPrimitiveArraysExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2);
    }

    /**
     * The test is inspired by the API customers
     */
    @Test
    public void testProvided3() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(ArrayOfComplexArraysExample.class);
        String dependencyClassPath = getDependencyClassPath();

        UtCompositeModel cClassModel = modelFactory.
                produceCompositeModel(
                        classIdForType(C.class),
                        Collections.singletonMap("integer", new UtPrimitiveModel(1))
                );

        UtCompositeModel bClassModel = modelFactory
                .produceCompositeModel(
                        classIdForType(B.class),
                        Collections.singletonMap("c", cClassModel)
                );

        UtCompositeModel demo9Model = modelFactory.
                produceCompositeModel(
                        classIdForType(Demo9.class),
                        Collections.singletonMap("b0", bClassModel)
                );

        EnvironmentModels environmentModels = new EnvironmentModels(
                demo9Model,
                Collections.singletonList(bClassModel),
                Collections.emptyMap()
        );

        Method methodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                Demo9.class,
                "test",
                B.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSets(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                environmentModels
                        )
                ),
                Demo9.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResult = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                Demo9.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet1);

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generate(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                environmentModels
                        )
                ),
                Collections.emptyList(),
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                Demo9.class
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2);
    }

    @Test
    public void testCustomAssertion() {
        String classpath = getClassPath(Trivial.class);
        String dependencyClassPath = getDependencyClassPath();

        UtCompositeModel model = modelFactory.
                produceCompositeModel(
                        classIdForType(Trivial.class)
                );

        EnvironmentModels environmentModels = new EnvironmentModels(
                model,
                Collections.singletonList(new UtPrimitiveModel(2)),
                Collections.emptyMap()
        );

        Method methodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                Trivial.class,
                "aMethod",
                int.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSets(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                environmentModels
                        )
                ),
                Trivial.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResult = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                Trivial.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet1);

        String generationResult2 = UtBotJavaApi.generate(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                environmentModels
                        )
                ),
                Collections.emptyList(),
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                Trivial.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResult2);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2);
    }

    /**
     * The test is inspired by the API customers
     */
    @Test
    public void testProvided3Reused() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(true);

        String dependencyClassPath = getDependencyClassPath();

        // The method in the class contains only one parameter
        String classSourceAsString =
                "public class Demo9Recompiled {" +
                        "    public int test(int b1) {" +
                        "        return 0;" +
                        "    }" +
                        "}";

        String demo9RecompiledClassName = "Demo9Recompiled";
        Pair<String, ClassLoader> classpathToClassLoader =
                compileClassAndGetClassPath(new Pair<>(demo9RecompiledClassName, classSourceAsString));


        String classpath = classpathToClassLoader.getFirst();
        ClassLoader classLoader = classpathToClassLoader.getSecond();

        Class<?> compiledClass = null;

        try {
            compiledClass = classLoader.loadClass(demo9RecompiledClassName);
        } catch (ClassNotFoundException e) {
            Assertions.fail("Failed to load a class; Classpath: " + classpathToClassLoader.getFirst());
        }

        if (compiledClass == null) {
            Assertions.fail("Failed to load the class");
        }

        UtCompositeModel demo9Model = modelFactory.
                produceCompositeModel(
                        classIdForType(compiledClass),
                        Collections.emptyMap()
                );

        EnvironmentModels environmentModels = new EnvironmentModels(
                demo9Model,
                Collections.singletonList(new UtPrimitiveModel(3)),
                Collections.emptyMap()
        );

        Method methodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                compiledClass,
                "test",
                int.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSets(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                environmentModels
                        )
                ),
                compiledClass,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                compiledClass,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet);

        // The test compiles and everything goes well.
        // Let's recompile the initial clas file

        // The method below has an extra parameter
        classSourceAsString =
                "public class Demo9Recompiled {" +
                        "    public int test(int b1, String s) {" +
                        "        return 0;" +
                        "    }" +
                        "}";

        Pair<String, ClassLoader> stringClassLoaderPair =
                compileClassAndGetClassPath(new Pair<>(demo9RecompiledClassName, classSourceAsString), classpath);

        ClassLoader reloadedClassLoader = stringClassLoaderPair.getSecond();

        Class<?> recompiledClass = null;

        try {
            recompiledClass = reloadedClassLoader.loadClass(demo9RecompiledClassName);
        } catch (ClassNotFoundException e) {
            Assertions.fail("Failed to load the class after recompilation; classpath: " + stringClassLoaderPair.getFirst());
        }

        if (recompiledClass == null) {
            Assertions.fail("Failed to load the class after recompilation");
        }

        EnvironmentModels environmentModels2 = new EnvironmentModels(
                demo9Model,
                Arrays.asList(new UtPrimitiveModel(4), new UtPrimitiveModel("Some String")),
                Collections.emptyMap()
        );

        Method methodUnderTest2 = PredefinedGeneratorParameters.getMethodByName(
                recompiledClass,
                "test",
                int.class,
                String.class
        );

        List<UtMethodTestSet> testSets1 = UtBotJavaApi.generateTestSets(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest2,
                                environmentModels
                        )
                ),
                recompiledClass,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResultWithConcreteExecutionOnly2 = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets1,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                recompiledClass,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly2);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2);
    }

    @Test
    public void testProvided1() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(ArrayOfComplexArraysExample.class);
        String dependencyClassPath = getDependencyClassPath();

        UtCompositeModel providedTestModel = modelFactory.produceCompositeModel(
                classIdForType(ProvidedExample.class),
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        List<UtModel> parameters = Arrays.asList(
                new UtPrimitiveModel(5),
                new UtPrimitiveModel(9),
                new UtPrimitiveModel("Some Text")
        );

        EnvironmentModels initialState = new EnvironmentModels(
                providedTestModel,
                parameters,
                Collections.emptyMap()
        );

        Method methodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                ProvidedExample.class,
                "test0",
                int.class, int.class, String.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSets(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState
                        )
                ),
                ProvidedExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResult = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                ProvidedExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet1);

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generate(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState
                        )
                ),
                Collections.emptyList(),
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                ProvidedExample.class
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2);
    }

    @Test
    public void testOnObjectWithArrayOfComplexArrays() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(ArrayOfComplexArraysExample.class);
        String dependencyClassPath = getDependencyClassPath();

        UtCompositeModel cmArrayOfComplexArrays = createArrayOfComplexArraysModel();

        UtCompositeModel testClassCompositeModel = modelFactory.produceCompositeModel(
                classIdForType(ArrayOfComplexArraysExample.class)
        );

        EnvironmentModels initialState = new EnvironmentModels(
                testClassCompositeModel,
                Collections.singletonList(cmArrayOfComplexArrays),
                Collections.emptyMap()
        );

        Method methodUnderTest = PredefinedGeneratorParameters.getMethodByName(
                ArrayOfComplexArraysExample.class,
                "getValue",
                ArrayOfComplexArrays.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSets(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState
                        )
                ),
                ArrayOfComplexArraysExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L
        );

        String generationResult = UtBotJavaApi.generate(
                Collections.emptyList(),
                testSets,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                ArrayOfComplexArraysExample.class,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet1);

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generate(
                Collections.singletonList(
                        new TestMethodInfo(
                                methodUnderTest,
                                initialState
                        )
                ),
                Collections.emptyList(),
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                ArrayOfComplexArraysExample.class
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2);
    }

    @Test
    public void testFuzzingSimple() {
        SootUtils.INSTANCE.runSoot(StringSwitchExample.class, false, new JdkInfoDefaultProvider().getInfo());
        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(StringSwitchExample.class);
        String dependencyClassPath = getDependencyClassPath();

        UtCompositeModel classUnderTestModel = modelFactory.produceCompositeModel(
                classIdForType(StringSwitchExample.class)
        );

        Method methodUnderTest = PredefinedGeneratorParameters.getMethodByName(StringSwitchExample.class, "validate", String.class, int.class, int.class);

        IdentityHashMap<UtModel, UtModel> models = modelFactory.produceAssembleModel(
                methodUnderTest,
                StringSwitchExample.class,
                Collections.singletonList(classUnderTestModel)
        );

        EnvironmentModels methodState = new EnvironmentModels(
                models.get(classUnderTestModel),
                Arrays.asList(new UtPrimitiveModel("initial model"), new UtPrimitiveModel(-10), new UtPrimitiveModel(0)),
                Collections.emptyMap()
        );

        TestMethodInfo methodInfo = new TestMethodInfo(
                methodUnderTest,
                methodState);
        List<UtMethodTestSet> testSets1 = UtBotJavaApi.fuzzingTestSets(
                Collections.singletonList(
                        methodInfo
                ),
                StringSwitchExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L,
                (type) -> {
                    if (int.class.equals(type) || Integer.class.equals(type)) {
                        return Arrays.asList(0, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    }
                    return null;
                }
        );

        String generate = UtBotJavaApi.generate(
                Collections.singletonList(methodInfo),
                testSets1,
                PredefinedGeneratorParameters.destinationClassName,
                classpath,
                dependencyClassPath,
                StringSwitchExample.class
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generate);
        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2);
    }

    @NotNull
    private String getClassPath(Class<?> clazz) {
        return clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    @NotNull
    private String getDependencyClassPath() {

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL[] urls = PathUtil.getUrlsFromClassLoader(contextClassLoader);


        return Arrays.stream(urls).map(url ->
        {
            try {
                return new File(url.toURI()).toString();
            } catch (URISyntaxException e) {
                Assertions.fail(e);
            }
            throw new RuntimeException();
        }).collect(Collectors.joining(File.pathSeparator));
    }
    public UtCompositeModel createArrayOfComplexArraysModel() {
        ClassId classIdOfArrayOfComplexArraysClass = classIdForType(ArrayOfComplexArrays.class);
        ClassId classIdOfComplexArray = classIdForType(ComplexArray.class);
        ClassId classIdOfPrimitiveFieldsClass = classIdForType(PrimitiveFields.class);

        ClassId classIdOfArrayOfPrimitiveFieldsClass = new ClassId("[L" + classIdOfPrimitiveFieldsClass.getCanonicalName() + ";", classIdOfPrimitiveFieldsClass);

        Map<Integer, UtModel> elementsOfComplexArrayArray = new HashMap<>();

        Map<Integer, UtModel> elementsWithPrimitiveFieldsClasses = new HashMap<>();

        elementsWithPrimitiveFieldsClasses.put(0, modelFactory.produceCompositeModel(
                classIdOfPrimitiveFieldsClass,
                Collections.singletonMap("a", new UtPrimitiveModel(5)),
                Collections.emptyMap()
        ));

        elementsWithPrimitiveFieldsClasses.put(1, modelFactory.produceCompositeModel(
                classIdOfPrimitiveFieldsClass,
                Collections.singletonMap("b", new UtPrimitiveModel(4)),
                Collections.emptyMap()
        ));

        UtArrayModel arrayOfPrimitiveFieldsModel = modelFactory.produceArrayModel(
                classIdOfArrayOfPrimitiveFieldsClass,
                2,
                new UtNullModel(classIdOfPrimitiveFieldsClass),
                elementsWithPrimitiveFieldsClasses
        );

        UtCompositeModel complexArrayClassModel = modelFactory.produceCompositeModel(
                classIdOfComplexArray,
                Collections.singletonMap("array", arrayOfPrimitiveFieldsModel)
        );

        elementsOfComplexArrayArray.put(1, complexArrayClassModel);

        ClassId classIdOfArraysOfComplexArrayClass = new ClassId("[L" + classIdOfComplexArray.getCanonicalName() + ";", classIdOfComplexArray);

        UtArrayModel arrayOfComplexArrayClasses = modelFactory.produceArrayModel(
                classIdOfArraysOfComplexArrayClass,
                2,
                new UtNullModel(classIdOfComplexArray),
                elementsOfComplexArrayArray
        );

        return modelFactory.produceCompositeModel(
                classIdOfArrayOfComplexArraysClass,
                Collections.singletonMap("array", arrayOfComplexArrayClasses));
    }
}