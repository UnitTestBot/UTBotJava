package org.utbot.examples.manual;

import kotlin.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.utbot.api.java.AbstractUtBotJavaApiTest;
import org.utbot.examples.assemble.DirectAccess;
import org.utbot.examples.assemble.PrimitiveFields;
import org.utbot.examples.assemble.ArrayOfComplexArrays;
import org.utbot.examples.assemble.ArrayOfPrimitiveArrays;
import org.utbot.examples.assemble.AssignedArray;
import org.utbot.examples.assemble.ComplexArray;
import org.utbot.examples.manual.examples.*;
import org.utbot.examples.manual.examples.customer.B;
import org.utbot.examples.manual.examples.customer.C;
import org.utbot.examples.manual.examples.customer.Demo9;
import org.utbot.external.api.TestMethodInfo;
import org.utbot.external.api.UnitTestBotLight;
import org.utbot.external.api.UtBotJavaApi;
import org.utbot.framework.codegen.domain.*;
import org.utbot.framework.context.simple.SimpleApplicationContext;
import org.utbot.framework.plugin.api.*;
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider;
import org.utbot.framework.util.Snippet;
import org.utbot.framework.util.SootUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.framework.plugin.api.MockFramework.MOCKITO;
import static org.utbot.framework.plugin.api.util.IdUtilKt.*;
import static org.utbot.framework.util.TestUtilsKt.compileClassAndGetClassPath;
import static org.utbot.framework.util.TestUtilsKt.compileClassFile;

/**
 * Tests for UnitTestBot Java API (Examples at the same time)
 */
public class UtBotJavaApiTest extends AbstractUtBotJavaApiTest {


    /** Uses {@link MultiMethodExample} as a class under test. Demonstrates how to gather information for multiple
     * methods analysis and pass it to {@link UtBotJavaApi#generateTestSetsForMethods} in order to produce set
     * of information needed for the test generation. After that shows how to use {@link UtBotJavaApi#generateTestCode}
     * in order to generate tests code.
     * <br>
     * The tests are generated with and without concrete execution.
     * <br>
     * Note, that you can use the {@code Snippet} instance in order to evaluate the test generation result.
     */
    @Test
    public void testMultiMethodClass() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(MultiMethodExample.class);
        String dependencyClassPath = getDependencyClassPath();

        UtCompositeModel classUnderTestModel = modelFactory.produceCompositeModel(
                classIdForType(MultiMethodExample.class)
        );

        Method firstMethodUnderTest = getMethodByName(MultiMethodExample.class, "firstMethod");
        Method secondMethodUnderTest = getMethodByName(MultiMethodExample.class, "secondMethod");
        Method thirdMethodUnderTest = getMethodByName(MultiMethodExample.class, "thirdMethod", String.class);

        // To find test sets, you need only {@link Method} instances
        // and some configuration

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSetsForMethods(
                Arrays.asList(
                        firstMethodUnderTest,
                        secondMethodUnderTest,
                        thirdMethodUnderTest
                ),
                MultiMethodExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L,
                new SimpleApplicationContext()
        );

        String generationResult = UtBotJavaApi.generateTestCode(
                emptyList(),
                testSets,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                MultiMethodExample.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                MultiMethodExample.class.getPackage().getName(),
                new SimpleApplicationContext()
        );

        TestMethodInfo firstTestMethodInfo = buildTestMethodInfo(
                firstMethodUnderTest,
                classUnderTestModel,
                emptyList(),
                Collections.emptyMap()
        );

        TestMethodInfo secondTestMethodInfo = buildTestMethodInfo(
                firstMethodUnderTest,
                classUnderTestModel,
                emptyList(),
                Collections.emptyMap()
        );

        TestMethodInfo thirdTestMethodInfo = buildTestMethodInfo(
                thirdMethodUnderTest,
                classUnderTestModel,
                Collections.singletonList(new UtPrimitiveModel("some")),
                Collections.emptyMap()
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet1);
        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generateTestCode(
                Arrays.asList(
                        firstTestMethodInfo,
                        secondTestMethodInfo,
                        thirdTestMethodInfo
                ),
                emptyList(),
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                MultiMethodExample.class,
                new SimpleApplicationContext()
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet2);
    }

    /** Uses {@link ClassRefExample} as a class under test. Demonstrates how to specify custom package name for the
     * generate tests.
     * <br>
     * The tests are generated with and without concrete execution.
     * <br>
     * Note, that you can use the {@code Snippet} instance in order to evaluate the test generation result.
     */
    @Test
    public void testCustomPackage() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(ClassRefExample.class);
        String dependencyClassPath = getDependencyClassPath();

        HashMap<String, UtModel> fields = new HashMap<>();
        fields.put("stringClass", modelFactory.produceClassRefModel(String.class));

        UtCompositeModel classUnderTestModel = modelFactory.produceCompositeModel(
                classIdForType(ClassRefExample.class),
                fields
        );

        UtClassRefModel classRefModel = modelFactory.produceClassRefModel(Class.class);

        Method methodUnderTest = getMethodByName(
                ClassRefExample.class,
                "assertInstance",
                Class.class);

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSetsForMethods(
                Collections.singletonList(methodUnderTest),
                ClassRefExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L,
                new SimpleApplicationContext()
        );

        String generationResult = UtBotJavaApi.generateTestCode(
                emptyList(),
                testSets,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                ClassRefExample.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                "some.custom.name",
                new SimpleApplicationContext()
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet1);

        TestMethodInfo testMethodInfo = buildTestMethodInfo(
                methodUnderTest,
                classUnderTestModel,
                Collections.singletonList(classRefModel),
                Collections.emptyMap()
        );

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generateTestCode(
                Collections.singletonList(testMethodInfo),
                emptyList(),
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                ClassRefExample.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                "some.custom.name",
                new SimpleApplicationContext()
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet2);
    }

    /** Uses {@link AssignedArrayExample} as a class under test. Demonstrates how to specify custom package name for the
     * generate tests.
     * <br>
     * The tests are generated with and without concrete execution.
     * <br>
     * Note, that you can use the {@code Snippet} instance in order to evaluate the test generation result.
     */
    @Test
    public void testOnObjectWithAssignedArrayField() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(AssignedArray.class);
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

        Method methodUnderTest = getMethodByName(
                AssignedArrayExample.class,
                "foo",
                AssignedArray.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSetsForMethods(
                Collections.singletonList(methodUnderTest),
                AssignedArrayExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L,
                new SimpleApplicationContext()
        );

        String generationResult = UtBotJavaApi.generateTestCode(
                emptyList(),
                testSets,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                AssignedArrayExample.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet1);

        TestMethodInfo methodInfo = buildTestMethodInfo(
                methodUnderTest,
                classUnderTestModel,
                Collections.singletonList(compositeModel),
                Collections.emptyMap()
        );

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generateTestCode(
                Collections.singletonList(methodInfo),
                emptyList(),
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                AssignedArrayExample.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet2);
    }

    /** Uses {@link DirectAccessExample} as a class under test. Demonstrates how to test objects with public fields.
     * <br>
     * The tests are generated with and without concrete execution.
     * <br>
     * Note, that you can use the {@code Snippet} instance in order to evaluate the test generation result.
     */
    @Test
    public void testObjectWithPublicFields() {

        UtBotJavaApi.setStopConcreteExecutorOnExit(false);

        String classpath = getClassPath(DirectAccessExample.class);
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

        Method methodUnderTest = getMethodByName(
                DirectAccessExample.class,
                "foo",
                DirectAccess.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSetsForMethods(
                Collections.singletonList(methodUnderTest),
                DirectAccessExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L,
                new SimpleApplicationContext()
        );

        String generationResult = UtBotJavaApi.generateTestCode(
                emptyList(),
                testSets,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                DirectAccessExample.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet1);

        TestMethodInfo methodInfo = buildTestMethodInfo(
                methodUnderTest,
                classUnderTestModel,
                Collections.singletonList(compositeModel),
                Collections.emptyMap()
        );

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generateTestCode(
                Collections.singletonList(methodInfo),
                emptyList(),
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                DirectAccessExample.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet2);
    }

    /** Uses {@link ArrayOfPrimitiveArraysExample} as a class under test. Demonstrates how to test code that uses arrays of primitives
     * inside arrays.
     * <br>
     * The tests are generated with and without concrete execution.
     * <br>
     * Note, that you can use the {@code Snippet} instance in order to evaluate the test generation result.
     */
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

        UtCompositeModel compositeModelArrayOfPrimitiveArrays = modelFactory.produceCompositeModel(
                classIdArrayOfPrimitiveArraysClass,
                Collections.singletonMap("array", enclosingArrayOfPrimitiveArrayModel)
        );

        UtCompositeModel classUnderTestModel = modelFactory.produceCompositeModel(
                cidArrayOfPrimitiveArraysTest
        );

        Method methodUnderTest = getMethodByName(
                ArrayOfPrimitiveArraysExample.class,
                "assign10",
                ArrayOfPrimitiveArrays.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSetsForMethods(
                Collections.singletonList(methodUnderTest),
                ArrayOfPrimitiveArraysExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L,
                new SimpleApplicationContext()
        );

        String generationResult = UtBotJavaApi.generateTestCode(
                emptyList(),
                testSets,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                ArrayOfPrimitiveArraysExample.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet);

        TestMethodInfo methodInfo = buildTestMethodInfo(
                methodUnderTest,
                classUnderTestModel,
                Collections.singletonList(compositeModelArrayOfPrimitiveArrays),
                Collections.emptyMap()
        );

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generateTestCode(
                Collections.singletonList(methodInfo),
                emptyList(),
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                ArrayOfPrimitiveArraysExample.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet2);
    }

    /** Example provided by customers.
     * <br>
     * The tests are generated with and without concrete execution.
     * <br>
     * Note, that you can use the {@code Snippet} instance in order to evaluate the test generation result.
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

        Method methodUnderTest = getMethodByName(
                Demo9.class,
                "test",
                B.class
        );


        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSetsForMethods(
                singletonList(methodUnderTest),
                Demo9.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L,
                new SimpleApplicationContext()
        );

        String generationResult = UtBotJavaApi.generateTestCode(
                emptyList(),
                testSets,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                Demo9.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet1);

        TestMethodInfo methodInfo = buildTestMethodInfo(
                methodUnderTest,
                demo9Model,
                Collections.singletonList(bClassModel),
                Collections.emptyMap()
        );


        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generateTestCode(
                Collections.singletonList(methodInfo),
                emptyList(),
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                Demo9.class,
                new SimpleApplicationContext()
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet2);
    }

    /** Trivial example.
     * <br>
     * The tests are generated with and without concrete execution.
     * <br>
     * Note, that you can use the {@code Snippet} instance in order to evaluate the test generation result.
     */
    @Test
    public void testCustomAssertion() {

        String classpath = getClassPath(Trivial.class);
        String dependencyClassPath = getDependencyClassPath();

        UtCompositeModel trivialModel = modelFactory.
                produceCompositeModel(
                        classIdForType(Trivial.class)
                );

        Method methodUnderTest = getMethodByName(
                Trivial.class,
                "aMethod",
                int.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSetsForMethods(
                Collections.singletonList(methodUnderTest),
                Trivial.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L,
                new SimpleApplicationContext()
        );

        String generationResult = UtBotJavaApi.generateTestCode(
                emptyList(),
                testSets,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                Trivial.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet1);

        TestMethodInfo methodInfo = buildTestMethodInfo(
                methodUnderTest,
                trivialModel,
                Collections.singletonList(new UtPrimitiveModel(2)),
                Collections.emptyMap()
        );

        String generationResult2 = UtBotJavaApi.generateTestCode(
                Collections.singletonList(methodInfo),
                emptyList(),
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                Trivial.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResult2);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet2);
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

        Method methodUderTest = getMethodByName(
                compiledClass,
                "test",
                int.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSetsForMethods(
                Collections.singletonList(methodUderTest),
                compiledClass,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L,
                new SimpleApplicationContext()
        );

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generateTestCode(
                emptyList(),
                testSets,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                compiledClass,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet);

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

        Method recompiledMethod = getMethodByName(
                recompiledClass,
                "test",
                int.class, String.class
        );

        List<UtMethodTestSet> testSets1 = UtBotJavaApi.generateTestSetsForMethods(
                Collections.singletonList(recompiledMethod),
                recompiledClass,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L,
                new SimpleApplicationContext()
        );

        String generationResultWithConcreteExecutionOnly2 = UtBotJavaApi.generateTestCode(
                emptyList(),
                testSets1,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                recompiledClass,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly2);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet2);
    }

    /** A test provided by our customer. Demonstrates how to use the API in
     * <br>
     * The tests are generated with and without concrete execution.
     * <br>
     * Note, that you can use the {@code Snippet} instance in order to evaluate the test generation result.
     */
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

        Method methodUnderTest = getMethodByName(
                ProvidedExample.class,
                "test0",
                int.class, int.class, String.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSetsForMethods(
                Collections.singletonList(methodUnderTest),
                ProvidedExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L,
                new SimpleApplicationContext()
        );

        String generationResult = UtBotJavaApi.generateTestCode(
                emptyList(),
                testSets,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                ProvidedExample.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet1);

        TestMethodInfo methodInfo = buildTestMethodInfo(
                methodUnderTest,
                providedTestModel,
                parameters,
                Collections.emptyMap()
        );

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generateTestCode(
                Collections.singletonList(methodInfo),
                emptyList(),
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                ProvidedExample.class,
                new SimpleApplicationContext()
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet2);
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

        Method methodUnderTest = getMethodByName(
                ArrayOfComplexArraysExample.class,
                "getValue",
                ArrayOfComplexArrays.class
        );

        List<UtMethodTestSet> testSets = UtBotJavaApi.generateTestSetsForMethods(
                Collections.singletonList(methodUnderTest),
                ArrayOfComplexArraysExample.class,
                classpath,
                dependencyClassPath,
                MockStrategyApi.OTHER_PACKAGES,
                3000L,
                new SimpleApplicationContext()
        );

        String generationResult = UtBotJavaApi.generateTestCode(
                emptyList(),
                testSets,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                ArrayOfComplexArraysExample.class,
                ProjectType.PureJvm,
                Junit4.INSTANCE,
                MOCKITO,
                CodegenLanguage.JAVA,
                MockitoStaticMocking.INSTANCE,
                false,
                ForceStaticMocking.DO_NOT_FORCE,
                new SimpleApplicationContext()
        );

        Snippet snippet1 = new Snippet(CodegenLanguage.JAVA, generationResult);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet1);


        TestMethodInfo methodInfo = buildTestMethodInfo(
                methodUnderTest,
                testClassCompositeModel,
                Collections.singletonList(cmArrayOfComplexArrays),
                Collections.emptyMap()
        );

        String generationResultWithConcreteExecutionOnly = UtBotJavaApi.generateTestCode(
                Collections.singletonList(methodInfo),
                emptyList(),
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                ArrayOfComplexArraysExample.class,
                new SimpleApplicationContext()
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generationResultWithConcreteExecutionOnly);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet2);
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

        Method methodUnderTest = getMethodByName(
                StringSwitchExample.class,
                "validate",
                String.class, int.class, int.class
        );

        List<UtMethodTestSet> testSets1 = UtBotJavaApi.fuzzingTestSets(
                Collections.singletonList(methodUnderTest),
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
                },
                new SimpleApplicationContext()
        );

        TestMethodInfo methodInfo = buildTestMethodInfo(
                methodUnderTest,
                classUnderTestModel,
                Arrays.asList(new UtPrimitiveModel("Some"), new UtPrimitiveModel(-10), new UtPrimitiveModel(0)),
                Collections.emptyMap()
        );

        String generate = UtBotJavaApi.generateTestCode(
                Collections.singletonList(methodInfo),
                testSets1,
                GENERATED_TEST_CLASS_NAME,
                classpath,
                dependencyClassPath,
                StringSwitchExample.class,
                new SimpleApplicationContext()
        );

        Snippet snippet2 = new Snippet(CodegenLanguage.JAVA, generate);
        compileClassFile(GENERATED_TEST_CLASS_NAME, snippet2);
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

    @Test
    public void testUnitTestBotLight() {
        String classpath = getClassPath(Trivial.class);
        String dependencyClassPath = getDependencyClassPath();

        UtCompositeModel model = modelFactory.
                produceCompositeModel(
                        classIdForType(Trivial.class)
                );

        Method methodUnderTest = getMethodByName(
                Trivial.class,
                "aMethod",
                int.class
        );

        TestMethodInfo methodInfo = buildTestMethodInfo(
                methodUnderTest,
                model,
                Collections.singletonList(new UtPrimitiveModel(2)),
                Collections.emptyMap()
        );

        UnitTestBotLight.run(
                (engine, state) -> System.err.println("Got a call:" + state.getStmt()),
                methodInfo,
                classpath,
                dependencyClassPath
        );
    }
}