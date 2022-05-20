package sbst.runtool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

public class SBSTChannel {
    private PrintWriter output;
    private BufferedReader input;

    public SBSTChannel(Reader input, Writer output) {
        this.input = new BufferedReader(input);
        this.output = new PrintWriter(output);
    }

    public void token(String string) throws IOException {
//        System.out.println("Await to receive token " + string);
        String line = input.readLine();
//        System.out.println("Received token " + line);
        if (!string.equals(line)) {
            throw new IOException("Unexpected: " + line + " expecting: " + string);
        }
    }

    public File directory() throws IOException {
        String line = input.readLine();
        File file = new File(line);
        if (file.exists() && file.isDirectory()) {
            return file;
        } else {
            throw new IOException("Not a valid directory name: " + line);
        }
    }

    public int number() throws IOException {
        String line = input.readLine();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            throw new IOException("Not a valid number: " + line);
        }
    }

    public long longnumber() throws IOException {
        String line = input.readLine();
        try {
            return Long.parseLong(line);
        } catch (NumberFormatException e) {
            throw new IOException("Not a valid longnumber: " + line);
        }
    }

    public File directory_jarfile() throws IOException {
        String line = input.readLine();
        File file = new File(line);
        if (file.exists()) {
            if (file.isDirectory() || (file.isFile() && file.getName().endsWith(".jar"))) {
                return file;
            } else {
                throw new IOException("Not a valid directory/jar file name: " + line);
            }
        } else {
            throw new IOException("File/Directory does not exist: " + line);
        }
    }

    public String className() throws IOException {
        String line = input.readLine();
        if (line.matches("[a-zA-Z_][a-zA-Z_0-9$]*(\\.[a-zA-Z_][a-zA-Z_0-9$]*)*")) {
            return line;
        } else {
            throw new IOException("Not a valid class name: " + line);
        }
    }

    public void emit(String string) {
        output.println(string);
        output.flush();
    }

    public void emit(int k) {
        emit("" + k);
    }

    public void emit(File file) {
        emit(file.getAbsolutePath());
    }

    public String readLine() throws IOException {
        return input.readLine();
    }

}
