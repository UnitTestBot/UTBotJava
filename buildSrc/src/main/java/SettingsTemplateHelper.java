import org.gradle.api.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.function.*;

/**
 * The purpose of this helper is to convert UtSettings.kt source code
 * to template resource file settings.properties (top-level entry in plugin JAR file).
 * There are two stages: parsing of input to build models and then rendering models to output file
 */

public class SettingsTemplateHelper {
    private static final String[] apacheLines =
            ("Copyright (c) " + new SimpleDateFormat("yyyy").format(System.currentTimeMillis()) + " utbot.org\n" +
                    "\n" +
                    "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                    "you may not use this file except in compliance with the License.\n" +
                    "You may obtain a copy of the License at\n" +
                    "\n" +
                    "    http://www.apache.org/licenses/LICENSE-2.0\n" +
                    "\n" +
                    "Unless required by applicable law or agreed to in writing, software\n" +
                    "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                    "See the License for the specific language governing permissions and\n" +
                    "limitations under the License.").split("\n");

    public static void proceed(Project project) {
        File settingsSourceDir = new File(project.getBuildDir().getParentFile().getParentFile(), "utbot-framework-api/src/main/kotlin/org/utbot/framework/");
        String sourceFileName = "UtSettings.kt";
        File settingsResourceDir = new File(project.getBuildDir().getParentFile().getParentFile(), "utbot-intellij-main/src/main/resources/");
        String settingsFileName = "settings.properties";

        Map<String, String> dictionary = new HashMap<>();
        dictionary.put("Int.MAX_VALUE", String.valueOf(Integer.MAX_VALUE));

        List<PropertyModel> models = new ArrayList<>();
        List<EnumInfo> enums = new ArrayList<>();
        StringBuilder acc = new StringBuilder();
        List<String> docAcc = new ArrayList<>();
        // Stage one: parsing sourcecode
        try(BufferedReader reader = new BufferedReader(new FileReader(new File(settingsSourceDir, sourceFileName)))) {
            for (String s = reader.readLine(); s != null; s = reader.readLine()) {
                s = s.trim();
                if (s.startsWith("enum class ")) {//Enum class declaration
                    enums.add(new EnumInfo(s.substring(11, s.length() - 2)));
                } else if (s.matches("[A-Z_]+,?") && !enums.isEmpty()) {//Enum value
                    String enumValue = s.substring(0, s.length());
                    if (enumValue.endsWith(",")) enumValue = enumValue.substring(0, enumValue.length() - 1);
                    enums.get(enums.size() - 1).docMap.put(enumValue, new ArrayList<>(docAcc));
                } else if (s.startsWith("const val ")) {//Constand value to be substitute later if need
                    int pos = s.indexOf(" = ");
                    dictionary.put(s.substring(10, pos), s.substring(pos + 3));
                } else if (s.equals("/**")) {//Start of docuemntation block
                    docAcc.clear();
                } else if (s.startsWith("* ")) {
                    if (!s.contains("href")) {//Links are not supported, skip them
                        docAcc.add(s.substring(2));
                    }
                } else if (s.startsWith("var") || s.startsWith("val")) {//Restart accumulation
                    acc.delete(0, acc.length());
                    acc.append(s);
                } else if (s.isEmpty() && acc.length() > 0) {//Build model from accumulated lines
                    s = acc.toString();
                    acc.delete(0, acc.length());
                    if (s.startsWith("var") || s.startsWith("val")) {
                        int i = s.indexOf(" by ", 3);
                        if (i > 0) {
                            String key = s.substring(3, i).trim();
                            if (key.contains(":")) {
                                key = key.substring(0, key.lastIndexOf(':'));
                            }
                            PropertyModel model = new PropertyModel(key);
                            models.add(model);
                            s = s.substring(i + 7);
                            i = s.indexOf("Property");
                            if (i > 0) model.type = s.substring(0, i);
                            if (i == 0) {
                                i = s.indexOf('<', i);
                                if (i != -1) {
                                    s = s.substring(i+1);
                                    i = s.indexOf('>');
                                    if (i != -1) {
                                        model.type = s.substring(0, i);
                                    }
                                }
                            }

                            i = s.indexOf('(', i);
                            if (i > 0) {
                                s = s.substring(i + 1);
                                String defaultValue = s.substring(0, s.indexOf(')')).trim();
                                if (defaultValue.contains(",")) defaultValue = defaultValue.substring(0, defaultValue.indexOf(','));
                                defaultValue = dictionary.getOrDefault(defaultValue, defaultValue);
                                if (defaultValue.matches("[\\d_]+L")) {
                                    defaultValue = defaultValue.substring(0, defaultValue.length() - 1).replace("_", "");
                                }
                                if (defaultValue.matches("^\".+\"$")) {
                                    defaultValue = defaultValue.substring(1, defaultValue.length() - 1);
                                }
                                model.defaultValue = defaultValue;
                                model.docLines.addAll(docAcc);
                            }
                        }
                    }
                } else if (acc.length() > 0) {
                    acc.append(" " + s);
                }
            }
        } catch (IOException ioe) {
            System.err.println("Unexpected error when processing " + sourceFileName);
            ioe.printStackTrace();
        }

        // Stage two: properties file rendering
        try (PrintWriter writer = new PrintWriter(new File(settingsResourceDir, settingsFileName))) {
            for (String apacheLine : apacheLines) {
                writer.println("# " + apacheLine);
            }
            for (PropertyModel model : models) {
                if (model.type.equals("Enum")) {
                    String[] split = model.defaultValue.split("\\.");
                    if (split.length > 1) {
                        model.defaultValue = split[1];
                        EnumInfo enumInfo = enums.stream().filter(new Predicate<EnumInfo>() {
                            @Override
                            public boolean test(EnumInfo enumInfo) {
                                return enumInfo.className.equals(split[0]);
                            }
                        }).findFirst().orElse(null);
                        if (enumInfo != null) {
                            model.docLines.add("");
                            for (Map.Entry<String, List<String>> entry : enumInfo.docMap.entrySet()) {
                                String enumValue = entry.getKey();
                                if (entry.getValue().size() == 1) {
                                    model.docLines.add(enumValue + ": " + entry.getValue().get(0));
                                } else {
                                    model.docLines.add(enumValue);
                                    for (String line : entry.getValue()) {
                                        model.docLines.add(line);
                                    }
                                }
                            }
                        }
                    }
                }
                writer.println();
                writer.println("#");
                for (String docLine : model.docLines) {
                    if (docLine.isEmpty()) {
                        writer.println("#");
                    } else {
                        writer.println("# " + docLine);
                    }
                }
                boolean defaultIsAlreadyMentioned = model.docLines.stream().anyMatch(new Predicate<String>() {
                    @Override
                    public boolean test(String s) {
                        return s.toLowerCase(Locale.ROOT).contains("default");
                    }
                });
                if (!defaultIsAlreadyMentioned) {
                    writer.println("#");
                    writer.println("# Default value is [" + model.defaultValue + "]");
                }
                writer.println("#" + model.key + "=" + model.defaultValue);
            }
            writer.flush();
            writer.close();
        } catch (IOException ioe) {
            System.err.println("Unexpected error when saving " + settingsFileName);
            ioe.printStackTrace();
        }
    }

    private static class PropertyModel {
        final String key;
        String type = "";
        String defaultValue = "";
        List<String> docLines = new ArrayList<>();

        PropertyModel(String key) {
            this.key = key;
        }
    }

    private static class EnumInfo {
        final String className;
        Map<String, List<String>> docMap = new LinkedHashMap<>();

        public EnumInfo(String className) {
            this.className = className;
        }
    }
}