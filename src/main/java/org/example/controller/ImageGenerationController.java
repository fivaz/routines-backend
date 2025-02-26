package org.example.controller;

import io.sentry.Sentry;
import org.example.dto.ImageRoutineGenerationRequest;
import org.example.service.RoutineImageGenerationService;
import org.example.service.TaskImageGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @GetMapping("/public/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("server is running");
    }

    @GetMapping("/public/error")
    public ResponseEntity<String> error() {
        try {
            throw new Exception("This is a test.");
        } catch (Exception e) {
            System.err.println("Error processing image generation: " + e.getMessage());
            Sentry.captureException(e);
        }

        return ResponseEntity.ok("error");
    }

    @PostMapping("/generate-task-image")
    public ResponseEntity<String> generateTaskImage(@RequestBody ImageTaskGenerationRequest request) {

        // Get the current authentication from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // The principal (user ID) is set during token validation in the filter
        String userId = (String) authentication.getPrincipal();

        // Access the parameters from the request body
        String taskId = request.getTaskId();
        String routineId = request.getRoutineId();
        String taskName = request.getTaskName();
        String focus = request.getFocus();

        // Start async processing (uncomment the line if necessary)
         taskImageGenerationService.generateAndStoreImage(userId, taskId, routineId, taskName, focus);

        // Return immediately
        return ResponseEntity.ok("waiting_image");
    }

    @PostMapping("/generate-routine-image")
    public ResponseEntity<String> generateRoutineImage(
            @RequestBody ImageRoutineGenerationRequest request) {

        // Get the current authentication from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // The principal (user ID) is set during token validation in the filter
        String userId = (String) authentication.getPrincipal();

        // Access the parameters from the request body
        String routineId = request.getRoutineId();
        String routineName = request.getRoutineName();

        // Start async processing (uncomment the line if necessary)
        routineImageGenerationService.generateAndStoreImage(userId, routineId, routineName);

        // Return immediately
        return ResponseEntity.ok("waiting_image");
    }
}