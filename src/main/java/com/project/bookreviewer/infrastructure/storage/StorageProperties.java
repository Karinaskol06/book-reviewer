package com.project.bookreviewer.infrastructure.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
// Filling the class from everything under storage in YAML
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    // Default values (fallbacks) if YAML doesn't set sth
    private String type = "local";
    private String publicBaseUrl = "";
    private Local local = new Local();
    private S3 s3 = new S3();

    // Default values for fields under local
    @Data
    public static class Local {
        private String uploadDir = "./uploads-book-reviewer";
        private String publicPrefix = "/uploads-book-reviewer";
    }

    // Default values for fields under s3
    @Data
    public static class S3 {
        private String bucket;
        private String region = "eu-central-1";
        private String endpoint = "";
        private String accessKey = "";
        private String secretKey = "";
        private boolean pathStyleAccess = false;
    }
}
