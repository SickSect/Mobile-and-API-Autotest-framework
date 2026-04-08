package org.ugina.utils;

import io.appium.java_client.android.AndroidDriver;

public class DriverManager {
    private static final ThreadLocal<AndroidDriver> driverThreadLocal = new ThreadLocal<>();

    public static void setDriver(AndroidDriver driver) {
        driverThreadLocal.set(driver);
    }

    public static AndroidDriver getDriver() {
        return driverThreadLocal.get();
    }

    public static void quitDriver() {
        if (driverThreadLocal.get() != null) {
            driverThreadLocal.get().quit();
            driverThreadLocal.remove(); // Очищаем поток
        }
    }
}
