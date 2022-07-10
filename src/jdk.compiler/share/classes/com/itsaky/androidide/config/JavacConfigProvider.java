package com.itsaky.androidide.config;

import javax.lang.model.SourceVersion;

public class JavacConfigProvider {

    /**
     * Property that can be set using <code>System.setProperty</code> to override
     * the default <code>java.home</code> property in compiler.
     */
    public static String PROP_ANDROIDIDE_JAVA_HOME = "androidide.java.home";

    /**
     * The latest source version that can be modeled.
     */
    private static SourceVersion latestSourceVersion = SourceVersion.RELEASE_11;

    /**
     * The latest source version supported by the compiler.
     */
    private static SourceVersion latestSupportedSourceVersion = latestSourceVersion;

    /**
     * Whether Java 9+ modules are enabled.
     */
    private static boolean modulesEnabled = true;

    /**
     * Gets the <code>androidide.java.home</code> property if it has been specified
     * else fallbacks to the <code>java.home</code> property.
     */
    public static String getJavaHome() {
        String javaHome = System.getProperty(PROP_ANDROIDIDE_JAVA_HOME);
        if (javaHome == null || javaHome.trim().length() == 0) {
            javaHome = System.getProperty("java.home");
        }
        return javaHome;
    }

    /**
     * Whether Java 9+ modules are enabled.
     */
    public static boolean isModulesEnabled() {
        return modulesEnabled;
    }

    /**
     * Disable Java 9+ modules.
     */
    public static void disableModules() {
        modulesEnabled = false;
    }

    /**
     * Enable Java 9+ modules.
     */
    public static void enableModules() {
        modulesEnabled = true;
    }

    /**
     * Get the latest source version that can be modeled.
     */
    public static SourceVersion getLatestSourceVersion() {
        return latestSourceVersion;
    }

    /**
     * Set the latest source version that can be modeled.
     */
    public static void setLatestSourceVersion(SourceVersion newVersion) {
        latestSourceVersion = newVersion;
    }

    /**
     * Set the latest source version supported by the compiler.
     */
    public static SourceVersion getLatestSupportedSourceVersion() {
        return latestSupportedSourceVersion;
    }

    /**
     * Set the latest source version supported by the compiler.
     */
    public static void setLatestSupportedSourceVersion(SourceVersion newVersion) {
        latestSupportedSourceVersion = newVersion;
    }
}
