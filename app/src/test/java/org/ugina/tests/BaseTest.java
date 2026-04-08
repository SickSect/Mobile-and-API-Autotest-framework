package org.ugina.tests;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.options.BaseOptions;
import io.qameta.allure.Attachment;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.MDC;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.ugina.utils.ConfigReader;
import org.ugina.utils.DriverManager;

import java.net.URL;
import java.time.Duration;

public class BaseTest {
    protected AndroidDriver driver;
    protected WebDriverWait wait;

    @BeforeClass
    public void setUp() throws Exception {
        var options = new BaseOptions()
                .amend("platformName", ConfigReader.get("appium.platformName"))
                .amend("appium:automationName", ConfigReader.get("appium.automationName"))
                .amend("appium:deviceName", ConfigReader.get("appium.deviceName"))
                .amend("appium:appPackage", ConfigReader.get("app.package"))
                .amend("appium:appActivity", ConfigReader.get("app.activity"))
                .amend("appium:noReset", ConfigReader.getBoolean("app.noReset"))
                .amend("appium:app", ConfigReader.get("app.path"))
                .amend("appium:newCommandTimeout", ConfigReader.getInt("appium.newCommandTimeout"));

        driver = new AndroidDriver(new URL(ConfigReader.get("appium.serverUrl")), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(ConfigReader.getInt("timeouts.implicitWait")));

        DriverManager.setDriver(driver);
        wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getInt("timeouts.explicitWait")));

        MDC.put("deviceName", "Pixel_6_API_34");
        MDC.put("platformVersion", "14");
    }

    @AfterMethod(alwaysRun = true)
    public void onTestFailure(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            attachScreenshot("Failure: " + result.getName());
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        MDC.clear();
        DriverManager.quitDriver();

    }

    @Attachment(value = "{name}", type = "image/png")
    public byte[] attachScreenshot(String name) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }
}
