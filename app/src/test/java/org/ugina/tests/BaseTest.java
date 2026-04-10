package org.ugina.tests;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Attachment;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.ugina.utils.ConfigReader;
import org.ugina.utils.DriverFactory;
import org.ugina.utils.DriverManager;

import java.time.Duration;

public class BaseTest {
    protected AndroidDriver driver;
    protected WebDriverWait wait;

    @BeforeClass
    public void setUp() throws Exception {
        String testMode = System.getProperty("test.mode", "local");
        boolean isCloud = testMode.equalsIgnoreCase("cloud");

        System.out.println("🚀 Starting in " + testMode.toUpperCase() + " mode");

        DriverFactory factory = new DriverFactory();
        AndroidDriver driver = factory.createDriver(isCloud);

        DriverManager.setDriver(driver);

        int timeout = isCloud ?
                ConfigReader.getInt("cloud.explicitWait") :
                ConfigReader.getInt("timeouts.explicitWait");
        wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));

        System.out.println("✅ Setup complete");
    }

    @AfterClass
    public void tearDown() {
        // Берем драйвер из ThreadLocal и закрываем
        DriverManager.quitDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void onTestFailure(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            attachScreenshot("Failure: " + result.getName());
        }
    }

    @Attachment(value = "{name}", type = "image/png")
    public byte[] attachScreenshot(String name) {
        if (driver == null) {
            System.out.println("⚠️  Cannot take screenshot: driver is null");
            return new byte[0];
        }
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }
}
