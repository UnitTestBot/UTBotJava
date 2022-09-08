package org.utbot.examples.mixed;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageIOUsage {
    public boolean isAlphaPremultiplied(File file) throws IOException {
        BufferedImage bimg = ImageIO.read(file);
        return bimg.isAlphaPremultiplied();
    }
}
