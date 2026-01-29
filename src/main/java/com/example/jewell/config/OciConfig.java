package com.example.jewell.config;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OciConfig {

    @Bean
    public ObjectStorage objectStorageClient() throws Exception {
        AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider("~/.oci/config", "DEFAULT");
        return ObjectStorageClient.builder()
                .build(provider);
    }
}