package com.project.bookreviewer.infrastructure.persistence.config;

import com.project.bookreviewer.infrastructure.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(storageProperties.getLocal().getUploadDir()).toAbsolutePath().normalize();
        String uploadLocation = uploadPath.toUri().toString();
        if (!uploadLocation.endsWith("/")) {
            uploadLocation = uploadLocation + "/";
        }

        String publicPrefix = storageProperties.getLocal().getPublicPrefix();
        if (!publicPrefix.startsWith("/")) {
            publicPrefix = "/" + publicPrefix;
        }

        registry.addResourceHandler(publicPrefix + "/**")
                .addResourceLocations(uploadLocation)
                .setCachePeriod(3600);
    }
}
