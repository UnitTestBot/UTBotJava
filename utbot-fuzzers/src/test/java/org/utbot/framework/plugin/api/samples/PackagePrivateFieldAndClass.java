package org.utbot.framework.plugin.api.samples;

@SuppressWarnings("All")
public class PackagePrivateFieldAndClass {

    volatile int pkgField = 0;

    PackagePrivateFieldAndClass() {

    }

    PackagePrivateFieldAndClass(int value) {
        pkgField = value;
    }

}
