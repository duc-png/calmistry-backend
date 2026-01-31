package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@lombok.extern.slf4j.Slf4j
public class FileController {

    private final com.cloudinary.Cloudinary cloudinary;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("============== FILE CONTROLLER INITIALIZED ==============");
    }

    @PostMapping("/upload")
    public ApiResponse<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        try {
            // Upload to Cloudinary
            java.util.Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    com.cloudinary.utils.ObjectUtils.emptyMap());

            // Get Secure URL (https)
            String fileUrl = (String) uploadResult.get("secure_url");

            return ApiResponse.<String>builder()
                    .result(fileUrl)
                    .build();
        } catch (Exception e) {
            // Catch all exceptions (IOException, RuntimeException from Cloudinary)
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }
}
