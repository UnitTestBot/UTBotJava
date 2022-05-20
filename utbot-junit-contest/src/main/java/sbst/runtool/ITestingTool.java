package sbst.runtool;

import java.io.File;
import java.util.List;

public interface ITestingTool {

    /**
     * List of additional class path entries required by a testing tool
     *
     * @return List of directories/jar files
     */
    public List<File> getExtraClassPath();

    /**
     * Initialize the testing tool, with details about the code to be tested (SUT)
     * Called only once.
     *
     * @param src       Directory containing source files of the SUT
     * @param bin       Directory containing class files of the SUT
     * @param classPath List of directories/jar files (dependencies of the SUT)
     */
    public void initialize(File src, File bin, List<File> classPath);

    /**
     * Run the test tool, and let it generate test cases for a given class
     *
     * @param cName      Name of the class for which unit tests should be generated
     * @param timeBudget How long the tool must run to test the class (in miliseconds)
     */
    public void run(String cName, long timeBudget);

}
