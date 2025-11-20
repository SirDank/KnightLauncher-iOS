package net.kdt.pojavlauncher.uikit;

import java.io.*;
import java.lang.reflect.*;
import java.util.jar.*;

import net.kdt.pojavlauncher.Tools;

public class UIKit {
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MOVE = 2;
    public static final int ACTION_MOVE_MOTION = 3;

    public static void callback_JavaGUIViewController_launchJarFile(final String filepath, String[] args)
            throws Throwable {
        // Launch the JAR file
        JarFile jarfile = new JarFile(filepath);
        String mainClass = jarfile.getManifest().getMainAttributes().getValue("Main-Class");
        jarfile.close();
        if (mainClass == null) {
            throw new IllegalArgumentException("no main manifest attribute, in \"" + filepath + "\"");
        }

        Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(mainClass);
        Method method = clazz.getMethod("main", String[].class);
        method.invoke(null, new Object[] { args });
    }

    public static void updateMCGuiScale() {
        // No-op
    }

    static {
        System.load(System.getenv("BUNDLE_PATH") + "/KnightLauncher");
    }

    // public static native void runOnUIThread(UIKitCallback callback);

    public static native void showError(String title, String message, boolean exitIfOk);

    public static native void updateProgress(String status, int progress, int max);
}
