package sbst.runtool;


import org.utbot.runtool.UtBotTool2;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

public class Main {

    //see 'runtool-sample.in' file for reference inpt
    public static void main(String[] args) throws IOException {
        Writer writer = new PrintWriter(System.out);
        Reader reader = new InputStreamReader(System.in);
        ITestingTool tool = new UtBotTool2();
        RunTool runTool = new RunTool(tool, reader, writer);
        runTool.run();
    }

}
