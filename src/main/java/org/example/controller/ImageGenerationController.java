package org.example.controller;

import io.sentry.Sentry;
import org.example.dto.ImageRoutineGenerationRequest;
import org.example.response.RecraftApiException;
import org.example.response.RecraftAuthenticationException;
import org.example.response.RecraftRateLimitException;
import org.example.service.RoutineImageGenerationService;
import org.example.service.TaskImageGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.example.dto.ImageTaskGenerationRequest;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

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

    @GetMapping("/sentry")
    public ResponseEntity<String> error() {
        try {
            throw new Exception("This is a test.");
        } catch (Exception e) {
            System.err.println("Error processing image generation: " + e.getMessage());
            Sentry.captureException(e);
        }

        return ResponseEntity.ok("error");
    }

    @GetMapping("/credits")
    public Mono<ResponseEntity<Map<String, Object>>> getRecraftCredits() {
        return taskImageGenerationService.getUserCredits()
                .map(credits -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("credits", credits);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(RecraftApiException.class, ex -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", ex.getMessage());

                    HttpStatus statusCode;
                    if (ex instanceof RecraftAuthenticationException) {
                        statusCode = HttpStatus.UNAUTHORIZED; // 401
                    } else if (ex instanceof RecraftRateLimitException) {
                        statusCode = HttpStatus.TOO_MANY_REQUESTS; // 429
                    } else {
                        statusCode = HttpStatus.INTERNAL_SERVER_ERROR; // 500
                    }

                    return Mono.just(ResponseEntity.status(statusCode).body(errorResponse));
                });
    }

    @PostMapping("/protected/generate-task-image")
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

        // Start async processing
        taskImageGenerationService.generateAndStoreImage(userId, taskId, routineId, taskName, focus);

        // Return immediately
        return ResponseEntity.ok("waiting_image");
    }

    @PostMapping("/protected/generate-routine-image")
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

@ControllerAdvice
class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final boolean isSentryEnabled;

    public GlobalExceptionHandler(@Value("${sentry.enabled:false}") boolean isSentryEnabled) {
        this.isSentryEnabled = isSentryEnabled;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception e) {
        logger.error("Unexpected error occurred: {}", e.getMessage(), e);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "An unexpected error occurred");
        errorResponse.put("details", e.getMessage()); // Optional: expose details in dev, hide in prod

        if (isSentryEnabled) {
            Sentry.captureException(e);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}