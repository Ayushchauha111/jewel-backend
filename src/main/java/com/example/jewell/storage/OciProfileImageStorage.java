package com.example.jewell.storage;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;

@Component
public class OciProfileImageStorage implements ProfileImageStorage {
    private static final Logger log = LoggerFactory.getLogger(OciProfileImageStorage.class);

    @Autowired
    private ObjectStorage objectStorageClient;

    @Value("${jewell.oci.profile-images.bucket:profile-images}")
    private String bucketName;

    @Value("${jewell.oci.namespace:bmm07pdrrgsg}")
    private String namespaceName;

    @Override
    public void upload(String objectName, InputStream inputStream) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucketName(bucketName)
                .namespaceName(namespaceName)
                .objectName(objectName)
                .putObjectBody(inputStream)
                .build();
        objectStorageClient.putObject(putObjectRequest);
    }

    @Override
    public void deleteIfExists(String objectName) {
        if (objectName == null || objectName.isBlank()) return;
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucketName(bucketName)
                    .namespaceName(namespaceName)
                    .objectName(objectName)
                    .build();
            objectStorageClient.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            // don't fail user flows due to best-effort cleanup
            log.warn("Failed to delete OCI object '{}': {}", objectName, e.getMessage());
        }
    }
}