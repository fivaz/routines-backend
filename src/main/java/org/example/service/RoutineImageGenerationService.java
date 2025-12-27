package org.example.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class RoutineImageGenerationService extends ImageGenerationAndStorageService {
    private static final Logger logger = LoggerFactory.getLogger(RoutineImageGenerationService.class);

    @Autowired
    public RoutineImageGenerationService(WebClient webClient, Firestore firestore,
                                         Bucket storageBucket) {
        super(webClient, firestore, storageBucket);
    }

    @Async
    public void generateAndStoreImage(String userId, String routineId, String routineName) {
        String path = String.format("users/%s/routines/%s", userId, routineId);

        CompletableFuture.runAsync(() -> {
            try {
                // Generate prompt using Gemini
                String prompt = generateGeminiPrompt(routineName);

                // Generate image using Recraft
                String imageUrl = generateRecraftImage(prompt);

                // Download and store image in Firebase Storage
                String imageStorageUrl = storeImage(imageUrl, path);

                // Update Firestore
                updateFirestore(path, imageStorageUrl);

            } catch (Exception e) {
                // Log error but don't throw it since this is async
                System.err.println("Error processing image generation: " + e.getMessage());

                updateFirestore(path, "error");
            }
        });
    }

    private String generateGeminiPrompt(String taskName) {
        String promptTemplate =
                "You are an expert in crafting vivid, engaging image prompts. Your task is to transform a simple task name into " +
                        "a rich, immersive image description that illustrates a person performing the task in an enjoyable and inviting way. " +
                        "- Clearly depict a person actively engaged in the task. " +
                        "- Unless the task explicitly specifies a different gender, assume the person is a man. " +
                        "- Emphasize body language and facial expressions that convey enjoyment, focus, or relaxation. " +
                        "- Describe the environment in a way that makes the scene feel pleasant, warm, or inspiring. " +
                        "- Keep the description under 1000 characters. " +
                        "Task: \"" + taskName + "\" Image Description:";

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", promptTemplate);
        content.put("parts", new Object[]{part});
        requestBody.put("contents", new Object[]{content});

        Map responseBody = webClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // Extract prompt from Gemini response
        return extractPromptFromGeminiResponse(responseBody);
    }
}