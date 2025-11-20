package net.kdt.pojavlauncher;

import java.beans.Beans;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

import org.lwjgl.glfw.CallbackBridge;
import org.lwjgl.glfw.GLFW;

import net.kdt.pojavlauncher.uikit.*;
import net.kdt.pojavlauncher.utils.*;
import net.kdt.pojavlauncher.value.*;

public class PojavLauncher {
    private static float currProgress, maxProgress;

    public static void main(String[] args) throws Throwable {
        // Skip calling to
        // com.apple.eawt.Application.nativeInitializeApplicationDelegate()
        Beans.setDesignTime(true);
        try {
            // Some places use macOS-specific code, which is unavailable on iOS
            // In this case, try to get it to use Linux-specific code instead.
            com.apple.eawt.Application.getApplication();
            Class clazz = Class.forName("com.apple.eawt.Application");
            Field field = clazz.getDeclaredField("sApplication");
            field.setAccessible(true);
            field.set(null, null);
            sun.font.FontUtilities.isLinux = true;
            System.setProperty("java.util.prefs.PreferencesFactory", "java.util.prefs.FileSystemPreferencesFactory");
        } catch (Throwable th) {
            // Not on JRE8, ignore exception
            // Tools.showError(th);
        }

        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            public void uncaughtException(Thread t, Throwable th) {
                th.printStackTrace();
                System.exit(1);
            }
        });

        try {
            // Try to initialize Caciocavallo17
            Class.forName("com.github.caciocavallosilano.cacio.ctc.CTCPreloadClassLoader");
        } catch (ClassNotFoundException e) {
        }

        if (args[0].equals("-jar")) {
            if (args[1].equals("knight_install")) {
                installSpiralKnights();
                return;
            }
            UIKit.callback_JavaGUIViewController_launchJarFile(args[1], Arrays.copyOfRange(args, 2, args.length));
        } else {
            launchMinecraft(args);
        }
    }

    public static void launchMinecraft(String[] args) throws Throwable {
        // Args for Spiral Knights
        System.setProperty("appdir", "./spiral");
        System.setProperty("resource_dir", "./spiral/rsrc");

        String sizeStr = System.getProperty("cacio.managed.screensize");
        System.setProperty("glfw.windowSize", sizeStr);

        System.setProperty("org.lwjgl.vulkan.libname", "libMoltenVK.dylib");

        MinecraftAccount account = MinecraftAccount.load(args[0]);
        JMinecraftVersionList.Version version = Tools.getVersionInfo(args[1]);
        System.out.println("Launching Spiral Knights " + version.id);

        Tools.launchMinecraft(account, version);
    }

    private static void installSpiralKnights() {
        new net.kdt.pojavlaunch.knight.KnightInstaller(new net.kdt.pojavlaunch.knight.Progress() {
            @Override
            public void postStepProgress(int prg) {
                UIKit.updateProgress(null, prg, -1);
            }

            @Override
            public void postPartProgress(int prg) {
                // UIKit.updateProgress(null, prg, -1);
            }

            @Override
            public void postMaxSteps(int max) {
                UIKit.updateProgress(null, -1, max);
            }

            @Override
            public void postMaxPart(int max) {
                // UIKit.updateProgress(null, -1, max);
            }

            @Override
            public void setPartIndeterminate(boolean indeterminate) {
                // UIKit.updateProgress(null, -1, -1);
            }

            @Override
            public void postLogLine(String line, Throwable th) {
                UIKit.updateProgress(line, -1, -1);
                if (th != null)
                    th.printStackTrace();
            }

            @Override
            public void moveToTop() {
            }

            @Override
            public void unlockExit() {
                UIKit.updateProgress("Done", 100, 100);
                System.exit(0);
            }
        }).run();
    }
}
