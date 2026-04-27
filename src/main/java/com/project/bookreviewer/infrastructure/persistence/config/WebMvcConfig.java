package com.project.bookreviewer.infrastructure.persistence.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.avatar.dir}")
    private String avatarSubDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /uploads-book-reviewer/** to the physical upload directory
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        String uploadLocation = uploadPath.toUri().toString();
        Path avatarsPath = uploadPath.resolve(avatarSubDir).normalize();
        String avatarsLocation = avatarsPath.toUri().toString();

        registry.addResourceHandler("/uploads-book-reviewer/**")
                .addResourceLocations(uploadLocation)
                .setCachePeriod(3600); // Cache for 1 hour

        // Backward-compatible mapping for already stored avatar URLs like /avatars/{file}
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(avatarsLocation)
                .setCachePeriod(3600);
    }
}