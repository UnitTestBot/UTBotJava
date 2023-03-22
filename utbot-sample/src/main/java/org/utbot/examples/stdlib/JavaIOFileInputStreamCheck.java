package org.utbot.examples.stdlib;

import java.io.FileInputStream;
import java.io.IOException;

public class JavaIOFileInputStreamCheck {
    public int read(String s) throws IOException {
        java.io.FileInputStream fis = new java.io.FileInputStream(s);
        byte[] b = new byte[1000];
        return fis.read(b);
    }
}
