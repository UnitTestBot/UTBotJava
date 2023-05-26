package org.utbot.engine.api;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class UTBotClassLoader extends URLClassLoader {
    @SuppressWarnings("FieldCanBeLocal")
    private final Class<?> launcherClass;
    @SuppressWarnings("FieldCanBeLocal")
    private final URL utbotJarPath;
    @SuppressWarnings("FieldCanBeLocal")
    private final String userClasspath;
    Predicate<String> apiClasses;

    public UTBotClassLoader(
            URL utbotJarPath,
            Class<?> launcherClass,
            String userClasspath,
            Predicate<String> apiClasses
    ) {
        super(new URL[0], ClassLoader.getSystemClassLoader());
        this.apiClasses = apiClasses;
        this.launcherClass = launcherClass;
        try {
            String classFileName = launcherClass.getCanonicalName().replace('.', '/') + ".class";
            ClassLoader cl = launcherClass.getClassLoader();
            String uri = Objects.requireNonNull(cl.getResource(classFileName)).toString();
            uri = uri.replace(classFileName, ""); // todo can contains several paths with this name, should be deleted only the last match
            URL resource = new URL(uri);

            addURL(resource);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot create " + getClass().getSimpleName(), e);
        }

        this.utbotJarPath = utbotJarPath;
        addURL(utbotJarPath);

        this.userClasspath = userClasspath;
        Stream<File> sootBuildDependencies = Arrays.stream(userClasspath.split(File.pathSeparator)).map(File::new);
        sootBuildDependencies.findFirst().ifPresent(file -> {
            try {
                addURL(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (apiClasses.test(name)) {
            return super.loadClass(name, resolve);
        }
        if (Objects.equals(name, getClass().getName())) {
            return getClass();
        }
        // Try to isolate all required classes from the loading from system classloader, e.g:
        // soot classes should be loaded separately by this class loader
        try {
            // do not try load class if it already exists
            Class<?> loadedClass = findLoadedClass(name);
            return loadedClass == null ? findClass(name) : loadedClass;
        } catch (ClassNotFoundException t) {
//            LOGGER.warn("Class is not found in UTBot classloader. Class will be loaded from system classloader", t);
        }
        return super.loadClass(name, resolve);
    }

    @SuppressWarnings("unused") // used in API
    public String getUserClasspath() {
        return userClasspath;
    }
}

