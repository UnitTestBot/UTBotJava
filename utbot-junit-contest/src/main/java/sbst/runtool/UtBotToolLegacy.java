package sbst.runtool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

//not used
@SuppressWarnings("unused")
public class UtBotToolLegacy implements ITestingTool {

    private PrintStream logOut = null;

    public UtBotToolLegacy() {
        super();
    }

    private boolean useClassList = true;

    public void setUseClassList(boolean isMandatory) {
        this.useClassList = isMandatory;
    }

    private File binFile;
    private List<File> classPathList;

    public List<File> getExtraClassPath() {
        return new ArrayList<File>();
    }

    public void initialize(File src, File bin, List<File> classPath) {
        this.binFile = bin;
        this.classPathList = classPath;
    }


    public void run(String cName, long timeBudget) {

        this.cleanLatestUtBotRetVal();

        final String homeDirName = "."; // current folder
        final String toolJarFile = "utbot-1.0.0.jar";
        new File(homeDirName + "/temp").mkdir();

        final String tempDirName = String.join(File.separator, homeDirName, "temp");
        File tempDir = new File(tempDirName);
        if (!tempDir.exists()) {
            throw new RuntimeException("Expected Temporary folder " + tempDirName + " was not found");
        }

        if (logOut == null) {
            final String logUtBotFileName = String.join(File.separator, tempDirName, "log_utbot.txt");
            PrintStream outStream;
            try {
                outStream = new PrintStream(new FileOutputStream(logUtBotFileName, true));
                this.setLoggingPrintWriter(outStream);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(
                        "FileNotFoundException signaled while creating output stream for  " + logUtBotFileName);
            }
        }

        log("Checking environment variable JAVA_HOME ");
        if (System.getenv("JAVA_HOME") == null) {
            throw new RuntimeException("JAVA_HOME must be configured in order to run this program");
        }

        log("Execution of tool UtBot STARTED");
        log("user.home=" + homeDirName);
        final String utBotJarFileName = String.join(File.separator, homeDirName, "lib", toolJarFile);

        File utbotJarFile = new File(utBotJarFileName);
        if (!utbotJarFile.exists()) {
            throw new RuntimeException("File utbot.jar was not found at folder "
                    + String.join(File.separator, homeDirName, "utbot", "dist"));
        }

        final String junitOutputDirName = String.join(File.separator, homeDirName, "temp", "testcases");
        File junitOutputDir = new File(junitOutputDirName);
        if (!junitOutputDir.exists()) {
            log("Creating directory " + junitOutputDir);
            junitOutputDir.mkdirs();
        }

        final String testClass = cName;
        final long timeLimit = timeBudget;

        final String junitPackageName;
        if (!testClass.contains(".")) {
            junitPackageName = null;
        } else {
            final int lastIndexOfDot = testClass.lastIndexOf(".");
            junitPackageName = testClass.substring(0, lastIndexOfDot);
        }

        final String classPath = createClassPath(binFile, classPathList, utbotJarFile);
        StringBuffer cmdLine = new StringBuffer();

        String javaCommand = buildJavaCommand();
        cmdLine.append(String.format("%s -cp %s org.utbot.contest.ContestKt", javaCommand, classPath));
        cmdLine.append(String.format(" %s", testClass));
        cmdLine.append(String.format(" %s", timeLimit));
        cmdLine.append(String.format(" %s", junitOutputDirName));

        final String regressionTestFileName;
        if (junitPackageName != null) {
            cmdLine.append(String.format(" %s", junitPackageName));

            regressionTestFileName = String.join(File.separator, junitOutputDirName,
                    junitPackageName.replace(".", File.separator), "RegressionTest.java");
        } else {
            regressionTestFileName = String.join(File.separator, junitOutputDirName, "RegressionTest.java");

        }


        String cmdToExecute = cmdLine.toString();

        File homeDir = new File(homeDirName);

        //REAL LAUNCH
        int retVal = launch(homeDir, cmdToExecute);


        log("UtBot execution finished with exit code " + retVal);
        this.setLatestUtBotRetVal(retVal);

        File regressionTestFile = new File(regressionTestFileName);
        if (regressionTestFile.exists()) {
            log("Deleting regression test suite file " + regressionTestFileName);
            regressionTestFile.delete();
        } else {
            log("Regression test suite file " + regressionTestFileName
                    + " could not be deleted since it does not exist");
        }

        log("Execution of tool UtBot FINISHED");
    }

    private void setLatestUtBotRetVal(int retVal) {
        this.latestUtBotRetVal = Integer.valueOf(retVal);
    }

    private void cleanLatestUtBotRetVal() {
        this.latestUtBotRetVal = null;
    }

    private String buildJavaCommand() {
        String cmd = String.join(File.separator, System.getenv("JAVA_HOME"), "bin", "java");
        return cmd;
    }

    private static String createClassPath(File subjectBinFile, List<File> subjectClassPathList, File utbotJarFile) {
        StringBuffer buff = new StringBuffer();
        buff.append(subjectBinFile.getAbsolutePath());
        for (File classPathFile : subjectClassPathList) {
            buff.append(File.pathSeparator);
            buff.append(classPathFile.getAbsolutePath());
        }
        buff.append(File.pathSeparator);
        buff.append(utbotJarFile.getAbsolutePath());
        return buff.toString();
    }

    public void setLoggingPrintWriter(PrintStream w) {
        this.logOut = w;
    }

    private void log(String msg) {
        if (logOut != null) {
            logOut.println(msg);
        }
    }

    private int launch(File baseDir, String cmdString) {
        DefaultExecutor executor = new DefaultExecutor();
        PumpStreamHandler streamHandler;
        streamHandler = new PumpStreamHandler(logOut, logOut, null);
        executor.setStreamHandler(streamHandler);
        if (baseDir != null) {
            executor.setWorkingDirectory(baseDir);
        }

        int exitValue;
        try {
            log("Spawning new process of command " + cmdString);
            exitValue = executor.execute(CommandLine.parse(cmdString));
            log("Execution of subprocess finished with ret_code " + exitValue);
            return exitValue;
        } catch (IOException e) {
            log("An IOException occurred during the execution of UtBot");
            return -1;
        }
    }

    private Integer latestUtBotRetVal = null;

    public Integer getLatestUtBotRetValue() {
        return latestUtBotRetVal;
    }

}
