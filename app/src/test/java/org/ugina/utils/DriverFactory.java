package org.ugina.utils;

import io.appium.java_client.android.AndroidDriver;

public class DriverFactory {

    private AndroidDriver driver;


    private static final ThreadLocal<AndroidDriver> driverThreadLocal = new ThreadLocal<>();

    public static void setDriver(AndroidDriver driver) {
        driverThreadLocal.set(driver);
    }

    public static AndroidDriver getDriver() {
        AndroidDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException("❌ Driver not initialized in thread: " + Thread.currentThread().getName());
        }
        return driver;
    }

    public static void quitDriver() {
        AndroidDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
        }
    }
}
