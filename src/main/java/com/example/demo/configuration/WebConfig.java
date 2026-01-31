package com.example.demo.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Expose the "dev-uploads" directory URL path "/files/**"
        Path uploadDir = Paths.get("dev-uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // Fix for Windows paths to ensure valid file URI
        if (!uploadPath.endsWith(File.separator)) {
            uploadPath += File.separator;
        }

        // Convert to URI properly to handle backslashes/forward slashes
        String resourceLocation = uploadDir.toUri().toString();

        registry.addResourceHandler("/files/**")
                .addResourceLocations(resourceLocation);
    }
}
