package org.ugina.ApiClient.Token;

import org.ugina.ApiClient.utils.Log;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores tokens per host. Thread-safe via ConcurrentHashMap.
 * Хранит токены по хостам. Потокобезопасен через ConcurrentHashMap.
 *
 * This is a storage class — it only stores and retrieves.
 * Token lifecycle (auth, refresh, retry) is handled by ApiTokenManager.
 *
 * Это класс-хранилище — только сохраняет и возвращает.
 * Жизненный цикл токенов (авторизация, обновление, повтор) — в ApiTokenManager.
 */
public class ApiTokenProvider {

    private static final Log log = Log.forClass(ApiTokenProvider.class);

    private static final ConcurrentHashMap<String, ApiTokenData> tokens = new ConcurrentHashMap<>();

    private ApiTokenProvider() {
    }

    /**
     * Stores token data for a host.
     * Сохраняет данные токена для хоста.
     */
    public static void register(ApiTokenData tokenData, String hostName) {
        tokens.put(hostName, tokenData);
        log.info("Token registered for '{}'", hostName);
    }

    /**
     * Returns token data for a host, or null if not registered.
     * Возвращает данные токена для хоста, или null если не зарегистрирован.
     */
    public static ApiTokenData get(String hostName) {
        return tokens.get(hostName);
    }

    /**
     * Checks if tokens exist for a host.
     * Проверяет, есть ли токены для хоста.
     */
    public static boolean has(String hostName) {
        return tokens.containsKey(hostName);
    }

    /**
     * Removes tokens for a host.
     * Удаляет токены для хоста.
     */
    public static void reset(String hostName) {
        tokens.remove(hostName);
        log.info("Token reset for '{}'", hostName);
    }

    /**
     * Removes all tokens.
     * Удаляет все токены.
     */
    public static void resetAll() {
        tokens.clear();
        log.info("All tokens reset");
    }
}