package org.ugina.utils;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.options.BaseOptions;
import org.openqa.selenium.Capabilities;
import java.net.URL;
import java.time.Duration;

public class DriverFactory {

    public AndroidDriver createDriver(boolean isCloud) {
        try {
            if (isCloud) {
                return createCloudDriver();
            } else {
                return createLocalDriver();
            }
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to create driver", e);
        }
    }

    private AndroidDriver createCloudDriver() throws Exception {
        System.out.println("🌐 Initializing CLOUD driver...");

        String username = getCloudUsername();
        String accessKey = getCloudAccessKey();

        if (username == null || username.isEmpty() ||
                accessKey == null || accessKey.isEmpty()) {
            System.out.println("⚠️  Cloud credentials not found. Falling back to LOCAL.");
            return createLocalDriver();
        }

        BaseOptions options = new BaseOptions()
                .amend("platformName", ConfigReader.get("cloud.platformName"))
                .amend("appium:automationName", ConfigReader.get("cloud.automationName"))
                .amend("appium:deviceName", ConfigReader.get("cloud.deviceName"))
                .amend("appium:platformVersion", ConfigReader.get("cloud.platformVersion"))
                .amend("appium:appPackage", ConfigReader.get("cloud.appPackage"))
                .amend("appium:appActivity", ConfigReader.get("cloud.appActivity"))
                .amend("appium:noReset", ConfigReader.getBoolean("cloud.noReset"))
                .amend("appium:build", ConfigReader.get("cloud.build"))
                .amend("appium:projectName", ConfigReader.get("cloud.projectName"))
                .amend("appium:w3c", true);

        String hubUrl = ConfigReader.get("cloud.hub")
                .replace("https://", "https://" + username + ":" + accessKey + "@");

        System.out.println("✅ Cloud driver initialized");
        return new AndroidDriver(new URL(hubUrl), options);
    }


    private AndroidDriver createLocalDriver() throws Exception {
        System.out.println("💻 Initializing LOCAL driver...");

        BaseOptions options = new BaseOptions()
                .amend("platformName", ConfigReader.get("appium.platformName"))
                .amend("appium:automationName", ConfigReader.get("appium.automationName"))
                .amend("appium:deviceName", ConfigReader.get("appium.deviceName"))
                .amend("appium:appPackage", ConfigReader.get("app.package"))
                .amend("appium:appActivity", ConfigReader.get("app.activity"))
                .amend("appium:noReset", ConfigReader.getBoolean("app.noReset"))
                .amend("appium:app", ConfigReader.get("app.path"))
                .amend("appium:newCommandTimeout", ConfigReader.getInt("appium.newCommandTimeout"));

        AndroidDriver driver = new AndroidDriver(
                new URL(ConfigReader.get("appium.serverUrl")),
                options
        );

        driver.manage().timeouts().implicitlyWait(
                Duration.ofSeconds(ConfigReader.getInt("timeouts.implicitWait"))
        );

        System.out.println("✅ Local driver initialized");
        return driver;
    }


    private String getCloudUsername() {
        String username = System.getenv("LT_USERNAME");
        if (username == null) username = System.getProperty("lt.username");
        if (username == null || username.startsWith("${")) {
            try { username = ConfigReader.get("cloud.username"); } catch (Exception e) {}
        }
        return username;
    }

    private String getCloudAccessKey() {
        String accessKey = System.getenv("LT_ACCESS_KEY");
        if (accessKey == null) accessKey = System.getProperty("lt.accessKey");
        if (accessKey == null || accessKey.startsWith("${")) {
            try { accessKey = ConfigReader.get("cloud.accessKey"); } catch (Exception e) {}
        }
        return accessKey;
    }
}