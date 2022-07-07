package com.sun.tools.javac.util;

public class JavaHomeProvider {

    public static String PROP_ANDROIDIDE_JAVA_HOME = "androidide.java.home";

    public static String getJavaHome() {
        String javaHome = System.getProperty(PROP_ANDROIDIDE_JAVA_HOME);
        if (javaHome == null || javaHome.trim().length() == 0) {
            javaHome = System.getProperty("java.home");
        }

        return javaHome;
    }
}
