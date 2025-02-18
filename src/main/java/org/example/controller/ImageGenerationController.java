package org.example.controller;

import org.example.service.ImageGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.dto.ImageGenerationRequest;

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
            @RequestBody ImageGenerationRequest request) {

        // Remove "Bearer " prefix if present
        String token = authToken.startsWith("Bearer ") ?
                authToken.substring(7) : authToken;

        // Access the parameters from the request body
        String taskId = request.getTaskId();
        String routineId = request.getRoutineId();
        String taskName = request.getTaskName();
        String focus = request.getFocus();

        // Start async processing (uncomment the line if necessary)
         imageGenerationService.generateAndStoreImage(token, taskId, routineId, taskName, focus);

        // Return immediately
        return ResponseEntity.ok("waiting_image");
    }
}