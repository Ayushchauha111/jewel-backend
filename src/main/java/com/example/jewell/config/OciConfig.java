package com.example.jewell.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OciConfig {

    private static final Logger log = LoggerFactory.getLogger(OciConfig.class);

    /**
     * OCI config file path. If set (e.g. via OCI_CONFIG_PATH on prod), this path is used.
     * Otherwise: use OCI_CONFIG_FILE env var if set, else ~/.oci/config (local).
     */
    @Value("${jewell.oci.config-file-path:}")
    private String configFilePath;

    @Bean
    public ObjectStorage objectStorageClient() throws Exception {
        AuthenticationDetailsProvider provider = buildAuthenticationProvider();
        return ObjectStorageClient.builder()
                .build(provider);
    }

    private AuthenticationDetailsProvider buildAuthenticationProvider() throws Exception {
        String pathToUse = (configFilePath != null && !configFilePath.isBlank())
                ? configFilePath.trim()
                : null;
        if (pathToUse == null) {
            pathToUse = System.getenv("OCI_CONFIG_FILE");
        }
        if (pathToUse == null || pathToUse.isBlank()) {
            pathToUse = System.getProperty("user.home") + "/.oci/config";
        }
        Path path = Paths.get(pathToUse).normalize();
        if (!path.toFile().exists()) {
            log.warn("OCI config file not found at {} - Object Storage uploads will fail with 401. " +
                    "Set jewell.oci.config-file-path or OCI_CONFIG_FILE on production.", path);
        }
        return new ConfigFileAuthenticationDetailsProvider(pathToUse, "DEFAULT");
    }
}