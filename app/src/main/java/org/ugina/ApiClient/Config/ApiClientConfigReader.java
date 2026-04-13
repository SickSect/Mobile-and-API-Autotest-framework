package org.ugina.ApiClient.Config;

import org.ugina.Data.PageDriverSetupData;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@SuppressWarnings("unchecked")
public class ApiClientConfigReader {
    private static final Properties props = new Properties();

    static {
        try (InputStream is = ApiClientConfigReader.class.getClassLoader()
                .getResourceAsStream("apiclient.properties")) {
            if (is == null) throw new RuntimeException("❌ Файл apiclient.properties не найден");
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("❌ Ошибка чтения конфига", e);
        }
    }

    public static String get(String key) {
        String value = props.getProperty(key);
        if (value == null) throw new RuntimeException("❌ Ключ не найден: " + key);
        return value;
    }

    public static boolean getBoolean(String key) { return Boolean.parseBoolean(get(key)); }
    public static int getInt(String key) { return Integer.parseInt(get(key)); }
}
