package org.utbot.python.framework.external;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.utbot.python.PythonTestSet;
import org.utbot.python.framework.codegen.model.Unittest;
import org.utbot.python.utils.Cleaner;
import org.utbot.python.utils.TemporaryFileManager;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PythonUtBotJavaApiTest {
    private final String pythonPath = "<Your path to Python executable file>";  // Set your path to Python before testing

    @BeforeEach
    public void setUp() {
        TemporaryFileManager.INSTANCE.initialize();
        Cleaner.INSTANCE.restart();
    }

    @AfterEach
    public void tearDown() {
        Cleaner.INSTANCE.doCleaning();
    }
    private File loadResource(String name) {
        URL resource = getClass().getClassLoader().getResource(name);
        if (resource == null) {
            throw new IllegalArgumentException("file not found!");
        } else {
            try {
                return new File(resource.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void testSimpleFunction() {
        File fileWithCode = loadResource("example_code/arithmetic.py");
        String pythonRunRoot = fileWithCode.getParentFile().getAbsolutePath();
        String moduleFilename = fileWithCode.getAbsolutePath();
        PythonObjectName testMethodName = new PythonObjectName("arithmetic", "calculate_function_value");
        PythonTestMethodInfo methodInfo = new PythonTestMethodInfo(testMethodName, moduleFilename, null);
        ArrayList<PythonTestMethodInfo> testMethods = new ArrayList<>(1);
        testMethods.add(methodInfo);
        ArrayList<String> directoriesForSysPath = new ArrayList<>();
        directoriesForSysPath.add(pythonRunRoot);
        String testCode = PythonUtBotJavaApi.generate(
                testMethods,
                pythonPath,
                pythonRunRoot,
                directoriesForSysPath,
                10_000,
                1_000,
                Unittest.INSTANCE
        );
        Assertions.assertFalse(testCode.isEmpty());
    }

    @Test
    public void testSimpleFunctionTestCase() {
        File fileWithCode = loadResource("example_code/arithmetic.py");
        String pythonRunRoot = fileWithCode.getParentFile().getAbsolutePath();
        String moduleFilename = fileWithCode.getAbsolutePath();
        PythonObjectName testMethodName = new PythonObjectName("arithmetic", "calculate_function_value");
        PythonTestMethodInfo methodInfo = new PythonTestMethodInfo(testMethodName, moduleFilename, null);
        ArrayList<PythonTestMethodInfo> testMethods = new ArrayList<>(1);
        testMethods.add(methodInfo);
        ArrayList<String> directoriesForSysPath = new ArrayList<>();
        directoriesForSysPath.add(pythonRunRoot);
        List<PythonTestSet> testCase = PythonUtBotJavaApi.generateTestSets(
                testMethods,
                pythonPath,
                pythonRunRoot,
                directoriesForSysPath,
                10_000,
                1_000
        );
        Assertions.assertFalse(testCase.isEmpty());
        Assertions.assertFalse(testCase.get(0).component2().isEmpty());
    }

    @Test
    public void testSimpleClassMethod() {
        File fileWithCode = loadResource("example_code/inner_dir/inner_module.py");
        File projectRoot = loadResource("example_code/");
        String pythonRunRoot = projectRoot.getAbsolutePath();
        String moduleFilename = fileWithCode.getAbsolutePath();
        PythonObjectName containingClassName = new PythonObjectName("inner_dir.inner_module", "InnerClass");
        PythonObjectName testMethodName = new PythonObjectName("inner_dir.inner_module", "f");
        PythonTestMethodInfo methodInfo = new PythonTestMethodInfo(testMethodName, moduleFilename, containingClassName);
        ArrayList<PythonTestMethodInfo> testMethods = new ArrayList<>(1);
        testMethods.add(methodInfo);
        ArrayList<String> directoriesForSysPath = new ArrayList<>();
        directoriesForSysPath.add(pythonRunRoot);
        String testCode = PythonUtBotJavaApi.generate(
                testMethods,
                pythonPath,
                pythonRunRoot,
                directoriesForSysPath,
                10_000,
                1_000,
                Unittest.INSTANCE
        );
        Assertions.assertFalse(testCode.isEmpty());
    }
}
