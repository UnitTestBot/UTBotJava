package org.utbot.examples.codegen;

// We can't access FileWithTopLevelFunctionsKt::class from Kotlin, so we use this class to get reflection from Java
public class FileWithTopLevelFunctionsReflectHelper {
    static Class<FileWithTopLevelFunctionsKt> clazz = FileWithTopLevelFunctionsKt.class;
}
