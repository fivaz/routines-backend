package org.example.controller;

import org.example.service.ImageGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/generate-image")
public class ImageGenerationController {

    private final ImageGenerationService imageGenerationService;

    public ImageGenerationController(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
    }

    @PostMapping
    public ResponseEntity<String> generateImage(
            @RequestHeader("Authorization") String authToken,
            @RequestParam String taskId,
            @RequestParam String routineId,
            @RequestParam String taskName,
            @RequestParam String focus) {

        // Remove "Bearer " prefix if present
        String token = authToken.startsWith("Bearer ") ?
                authToken.substring(7) : authToken;

        // Start async processing
//        imageGenerationService.generateAndStoreImage(token, taskId, routineId, taskName, focus);

        System.out.println(token);

        // Return immediately
        return ResponseEntity.ok("waiting_image1");
    }
}