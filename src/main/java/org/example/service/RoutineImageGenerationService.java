package org.example.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class RoutineImageGenerationService extends ImageGenerationAndStorageService {

    private final FirebaseAuth firebaseAuth;

    @Autowired
    public RoutineImageGenerationService(RestTemplate restTemplate, Firestore firestore,
                                         Bucket storageBucket, FirebaseAuth firebaseAuth) {
        super(restTemplate, firestore, storageBucket);
        this.firebaseAuth = firebaseAuth;
    }

    @Async
    public CompletableFuture<Void> generateAndStoreImage(String token, String routineId, String routineName) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Verify Firebase token
                FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
                String userId = decodedToken.getUid();

                // Generate prompt using Gemini
                String prompt = generateGeminiPrompt(routineName);

                // Generate image using Recraft
                String imageUrl = generateRecraftImage(prompt);

                // Download and store image in Firebase Storage
                String path = String.format("users/%s/routines/%s", userId, routineId);

                String imageStorageUrl = storeImage(imageUrl, path);

                // Update Firestore
                updateFirestore(path, imageStorageUrl);

            } catch (Exception e) {
                // Log error but don't throw it since this is async
                System.err.println("Error processing image generation: " + e.getMessage());
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
                        "Task: \"" + taskName + "\" Image Description:" ;


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", promptTemplate);
        content.put("parts", new Object[]{part});
        requestBody.put("contents", new Object[]{content});

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey,
                HttpMethod.POST,
                request,
                Map.class
        );

        // Extract prompt from Gemini response
        return extractPromptFromGeminiResponse(response.getBody());
    }
}