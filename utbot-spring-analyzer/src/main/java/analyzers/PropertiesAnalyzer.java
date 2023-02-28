package analyzers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class PropertiesAnalyzer {

    private final String propertiesFilePath;

    public PropertiesAnalyzer(String propertiesFilePath) {
        this.propertiesFilePath = propertiesFilePath;
    }

    public ArrayList<String> readProperties() throws IOException {
        ArrayList<String> props = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader(propertiesFilePath));
        String line = reader.readLine();
        while (line != null) {
            props.add(line);
            line = reader.readLine();
        }

        reader.close();

        return props;
    }
}
