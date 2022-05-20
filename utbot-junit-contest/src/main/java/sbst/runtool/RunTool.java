package sbst.runtool;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class RunTool {
    private final ITestingTool tool;
    private final SBSTChannel channel;

    public RunTool(ITestingTool tool, Reader input, Writer output) {
        this.tool = tool;
        this.channel = new SBSTChannel(input, output);
    }

    public void run() throws IOException {
        channel.token("BENCHMARK");
        File src = channel.directory();
        File bin = channel.directory();
        int n = channel.number();
        List<File> classPath = new ArrayList<File>();
        for (int i = 0; i < n; i++) {
            classPath.add(channel.directory_jarfile());
        }
        tool.initialize(src, bin, classPath);

        int m = channel.number();
        if (tool.getExtraClassPath() != null) {
            channel.emit("CLASSPATH");
            List<File> extraCP = tool.getExtraClassPath();
            int k = extraCP.size();
            channel.emit(k);
            for (File file : extraCP) {
                channel.emit(file);
            }
        }
        channel.emit("READY");
        for (int i = 0; i < m; i++) {
            long timeBudget = channel.longnumber();
            String cName = channel.className();
            tool.run(cName, timeBudget);
            channel.emit("READY");
        }
    }
}
