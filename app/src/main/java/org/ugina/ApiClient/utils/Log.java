package org.ugina.ApiClient.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final Logger logger;

    private Log(Logger logger) {
        this.logger = logger;
    }

    public static Log forClass(Class<?> clazz) {
        return new Log(LoggerFactory.getLogger(clazz));
    }

    public void info(String message, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(format("INFO", message, args));
        }
    }

    public void warn(String message, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(format("WARN", message, args));
        }
    }

    public void error(String message, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(format("ERROR", message, args));
        }
    }

    public void error(String message, Throwable throwable) {
        if (logger.isErrorEnabled()) {
            logger.error(format("ERROR", message), throwable);
        }
    }

    public void debug(String message, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(format("DEBUG", message, args));
        }
    }

    private String format(String level, String message, Object... args) {
        // Подставляем аргументы в плейсхолдеры {}
        String resolved = replacePlaceholders(message, args);

        // Собираем итоговую строку
        return String.format("[%s] [%s] [%s] → %s",
                LocalDateTime.now().format(TIME_FORMAT),
                level,
                Thread.currentThread().getName(),
                resolved);
    }

    private String replacePlaceholders(String template, Object... args) {
        if (args == null || args.length == 0) return template;

        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int i = 0;

        while (i < template.length()) {
            if (i < template.length() - 1
                    && template.charAt(i) == '{'
                    && template.charAt(i + 1) == '}'
                    && argIndex < args.length) {

                sb.append(args[argIndex] != null ? args[argIndex].toString() : "null");
                argIndex++;
                i += 2;
            } else {
                sb.append(template.charAt(i));
                i++;
            }
        }

        return sb.toString();
    }
}