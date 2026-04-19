package org.ugina.ApiClient.Config;

import org.ugina.ApiClient.utils.Log;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;

/**
 * Читает сертификаты из ресурсов проекта и создаёт SSLContext.
 *
 * ВАЖНО: при указании кастомного TrustStore, он ОБЪЕДИНЯЕТСЯ с системным.
 * Без этого обычные HTTPS-запросы (jsonplaceholder, google и т.д.) перестанут работать,
 * потому что Java будет искать их CA только в нашем truststore.
 *
 * Использование:
 *
 *   // Только TrustStore (самоподписанный сервер)
 *   SslConfig ssl = SslConfig.builder()
 *       .trustStore("certs/truststore.p12", "trustpass")
 *       .build();
 *
 *   // Полный mTLS
 *   SslConfig ssl = SslConfig.builder()
 *       .keyStore("certs/client.p12", "keypass")
 *       .trustStore("certs/truststore.p12", "trustpass")
 *       .build();
 */
public class SslConfig {

    private static final Log log = Log.forClass(SslConfig.class);

    private String keyStorePath;
    private char[] keyStorePassword;
    private String keyStoreType;

    private String trustStorePath;
    private char[] trustStorePassword;
    private String trustStoreType;

    private SslConfig() {
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Собирает SSLContext из указанных хранилищ.
     */
    public SSLContext createSslContext() {
        try {
            log.info("══════ SSL CONFIG START ══════");

            // ── KeyStore (наш сертификат) ──
            KeyManagerFactory keyManagerFactory = null;
            if (keyStorePath != null) {
                log.info("Loading KeyStore: {}", keyStorePath);
                KeyStore keyStore = loadStore(keyStorePath, keyStorePassword, keyStoreType);
                logStoreContents("KeyStore", keyStore);

                keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyStorePassword);
                log.info("KeyStore initialized — client certificate ready");
            } else {
                log.info("KeyStore: not configured (no client certificate)");
            }

            // ── TrustStore (кому доверяем) ──
            TrustManagerFactory trustManagerFactory = null;
            if (trustStorePath != null) {
                log.info("Loading TrustStore: {}", trustStorePath);
                KeyStore customTrustStore = loadStore(trustStorePath, trustStorePassword, trustStoreType);
                logStoreContents("Custom TrustStore", customTrustStore);

                // Загружаем системный truststore (cacerts из JDK).
                // Он содержит все публичные CA: Let's Encrypt, DigiCert, Cloudflare и т.д.
                // Без него запросы к обычным HTTPS-сайтам сломаются.
                KeyStore systemTrustStore = loadSystemTrustStore();

                // Объединяем: создаём новый KeyStore, копируем туда ВСЕ записи
                // из системного + все записи из нашего кастомного.
                KeyStore mergedTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                mergedTrustStore.load(null, null);  // инициализируем пустой

                // Сначала копируем системные сертификаты
                int systemCount = 0;
                for (String alias : Collections.list(systemTrustStore.aliases())) {
                    mergedTrustStore.setCertificateEntry(alias, systemTrustStore.getCertificate(alias));
                    systemCount++;
                }
                log.info("Merged {} system CA certificates", systemCount);

                // Потом добавляем наши кастомные (с префиксом чтобы не было коллизий имён)
                int customCount = 0;
                for (String alias : Collections.list(customTrustStore.aliases())) {
                    mergedTrustStore.setCertificateEntry("custom_" + alias, customTrustStore.getCertificate(alias));
                    customCount++;
                }
                log.info("Merged {} custom certificates", customCount);
                log.info("Total trusted certificates: {}", mergedTrustStore.size());

                trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(mergedTrustStore);
                log.info("TrustStore initialized — system + custom certificates loaded");
            } else {
                log.info("TrustStore: not configured (using system defaults)");
            }

            // ── Собираем SSLContext ──
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                    keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null,
                    trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null,
                    null
            );

            log.info("SSLContext created — protocol: {}", sslContext.getProtocol());
            log.info("══════ SSL CONFIG END ══════");

            return sslContext;

        } catch (Exception e) {
            log.error("Failed to create SSLContext: {}", e.getMessage());
            throw new SslConfigException("Failed to create SSLContext", e);
        }
    }

    /**
     * Загружает системный truststore (cacerts) из JDK.
     *
     * TrustManagerFactory.getInstance() + .init(null) — стандартный способ
     * получить дефолтный truststore Java. Он содержит ~100+ корневых CA.
     *
     * Мы достаём его через TrustManagerFactory, потому что путь к cacerts
     * различается между JDK-вендорами (Oracle, AdoptOpenJDK, GraalVM...).
     * Этот способ работает везде.
     */
    private KeyStore loadSystemTrustStore() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);  // null = использовать системный

            // Извлекаем сертификаты из системного TrustManager
            KeyStore systemStore = KeyStore.getInstance(KeyStore.getDefaultType());
            systemStore.load(null, null);

            // Получаем все доверенные сертификаты через TrustManager
            for (var tm : tmf.getTrustManagers()) {
                if (tm instanceof javax.net.ssl.X509TrustManager x509tm) {
                    int i = 0;
                    for (X509Certificate cert : x509tm.getAcceptedIssuers()) {
                        systemStore.setCertificateEntry("system_ca_" + i, cert);
                        i++;
                    }
                }
            }

            log.debug("System truststore loaded: {} entries", systemStore.size());
            return systemStore;

        } catch (Exception e) {
            log.warn("Could not load system truststore: {}. Custom certs only.", e.getMessage());
            try {
                KeyStore empty = KeyStore.getInstance(KeyStore.getDefaultType());
                empty.load(null, null);
                return empty;
            } catch (Exception ex) {
                throw new SslConfigException("Failed to create fallback truststore", ex);
            }
        }
    }

    /**
     * Логирует содержимое хранилища.
     */
    private void logStoreContents(String storeName, KeyStore store) {
        try {
            log.info("{} contains {} entries:", storeName, store.size());

            for (String alias : Collections.list(store.aliases())) {
                if (store.isKeyEntry(alias)) {
                    Certificate cert = store.getCertificate(alias);
                    if (cert instanceof X509Certificate x509) {
                        log.info("  [{}] type=PrivateKeyEntry (CLIENT CERT)", alias);
                        log.info("    Subject : {}", x509.getSubjectX500Principal().getName());
                        log.info("    Issuer  : {}", x509.getIssuerX500Principal().getName());
                        log.info("    Valid   : {} → {}", x509.getNotBefore(), x509.getNotAfter());
                        log.info("    Serial  : {}", x509.getSerialNumber());
                    }
                } else if (store.isCertificateEntry(alias)) {
                    Certificate cert = store.getCertificate(alias);
                    if (cert instanceof X509Certificate x509) {
                        log.info("  [{}] type=trustedCertEntry (TRUSTED)", alias);
                        log.info("    Subject : {}", x509.getSubjectX500Principal().getName());
                        log.info("    Issuer  : {}", x509.getIssuerX500Principal().getName());
                        log.info("    Valid   : {} → {}", x509.getNotBefore(), x509.getNotAfter());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not log {} contents: {}", storeName, e.getMessage());
        }
    }

    /**
     * Загружает хранилище из classpath (resources).
     */
    private KeyStore loadStore(String resourcePath, char[] password, String type) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new SslConfigException(
                        "Certificate file not found in resources: " + resourcePath
                                + "\nExpected location: src/test/resources/" + resourcePath);
            }

            KeyStore store = KeyStore.getInstance(type);
            store.load(is, password);
            log.debug("Loaded {} store from: {}", type, resourcePath);
            return store;

        } catch (SslConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new SslConfigException(
                    "Failed to load certificate store: " + resourcePath
                            + "\nPossible causes: wrong password, corrupted file, wrong format (expected " + type + ")",
                    e);
        }
    }

    // ──── Builder ────

    public static class Builder {

        private final SslConfig config = new SslConfig();

        public Builder keyStore(String resourcePath, String password) {
            config.keyStorePath = resourcePath;
            config.keyStorePassword = password.toCharArray();
            config.keyStoreType = detectStoreType(resourcePath);
            return this;
        }

        public Builder trustStore(String resourcePath, String password) {
            config.trustStorePath = resourcePath;
            config.trustStorePassword = password.toCharArray();
            config.trustStoreType = detectStoreType(resourcePath);
            return this;
        }

        public Builder keyStoreType(String type) {
            config.keyStoreType = type;
            return this;
        }

        public Builder trustStoreType(String type) {
            config.trustStoreType = type;
            return this;
        }

        public SslConfig build() {
            if (config.keyStorePath == null && config.trustStorePath == null) {
                throw new SslConfigException(
                        "At least one of keyStore or trustStore must be specified");
            }
            return config;
        }

        private String detectStoreType(String path) {
            String lower = path.toLowerCase();
            if (lower.endsWith(".p12") || lower.endsWith(".pfx")) {
                return "PKCS12";
            } else if (lower.endsWith(".jks")) {
                return "JKS";
            }
            return "PKCS12";
        }
    }

    public static class SslConfigException extends RuntimeException {
        public SslConfigException(String message) {
            super(message);
        }

        public SslConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}