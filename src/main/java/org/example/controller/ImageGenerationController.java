package org.example.controller;

import io.sentry.Sentry;
import org.example.dto.ImageRoutineGenerationRequest;
import org.example.service.RoutineImageGenerationService;
import org.example.service.TaskImageGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.dto.ImageTaskGenerationRequest;

@RestController
@RequestMapping
public class ImageGenerationController {

    private final TaskImageGenerationService taskImageGenerationService;
    private final RoutineImageGenerationService routineImageGenerationService;

    public ImageGenerationController(TaskImageGenerationService taskImageGenerationService, RoutineImageGenerationService routineImageGenerationService) {
        this.taskImageGenerationService = taskImageGenerationService;
        this.routineImageGenerationService = routineImageGenerationService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("server is running");
    }

    @GetMapping("/error1")
    public ResponseEntity<String> error1() {
        try {
            throw new Exception("This is a test.");
        } catch (Exception e) {
            System.err.println("Error processing image generation: " + e.getMessage());
            Sentry.captureException(e);
        }

        return ResponseEntity.ok("error");
    }

    @PostMapping("/generate-task-image")
    public ResponseEntity<String> generateTaskImage(
            @RequestHeader("Authorization") String authToken,
            @RequestBody ImageTaskGenerationRequest request) {

        // Remove "Bearer " prefix if present
        String token = authToken.startsWith("Bearer ") ?
                authToken.substring(7) : authToken;

        // Access the parameters from the request body
        String taskId = request.getTaskId();
        String routineId = request.getRoutineId();
        String taskName = request.getTaskName();
        String focus = request.getFocus();

        // Start async processing (uncomment the line if necessary)
         taskImageGenerationService.generateAndStoreImage(token, taskId, routineId, taskName, focus);

        // Return immediately
        return ResponseEntity.ok("waiting_image");
    }

    @PostMapping("/generate-routine-image")
    public ResponseEntity<String> generateRoutineImage(
            @RequestHeader("Authorization") String authToken,
            @RequestBody ImageRoutineGenerationRequest request) {

        // Remove "Bearer " prefix if present
        String token = authToken.startsWith("Bearer ") ?
                authToken.substring(7) : authToken;

        // Access the parameters from the request body
        String routineId = request.getRoutineId();
        String routineName = request.getRoutineName();

        // Start async processing (uncomment the line if necessary)
        routineImageGenerationService.generateAndStoreImage(token, routineId, routineName);

        // Return immediately
        return ResponseEntity.ok("waiting_image");
    }
}