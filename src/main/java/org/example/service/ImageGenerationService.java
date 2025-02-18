package org.example.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.util.concurrent.TimeUnit;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ImageGenerationService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${recraft.api.key}")
    private String recraftApiKey;

    @Value("${recraft.style.id}")
    private String styleId;

    private final RestTemplate restTemplate;
    private final Firestore firestore;
    private final Bucket storageBucket;
    private final FirebaseAuth firebaseAuth;

    @Autowired
    public ImageGenerationService(RestTemplate restTemplate, Firestore firestore,
                                  Bucket storageBucket, FirebaseAuth firebaseAuth) {
        this.restTemplate = restTemplate;
        this.firestore = firestore;
        this.storageBucket = storageBucket;
        this.firebaseAuth = firebaseAuth;
    }

    @Async
    public CompletableFuture<Void> generateAndStoreImage(String token, String taskId, String routineId, String taskName, String focus) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Verify Firebase token
                FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
                String userId = decodedToken.getUid();

                // Generate prompt using Gemini
                String prompt = generateGeminiPrompt(taskName, focus);

                // Generate image using Recraft
                String imageUrl = generateRecraftImage(prompt);

                // Download and store image in Firebase Storage
                String storagePath = String.format("users/%s/routines/%s/tasks/%s",
                        userId, routineId, taskId);
                String imageStorageUrl = storeImage(imageUrl, storagePath);

                // Update Firestore
                updateFirestore(userId, routineId, taskId, imageStorageUrl);

            } catch (Exception e) {
                // Log error but don't throw it since this is async
                System.err.println("Error processing image generation: " + e.getMessage());
            }
        });
    }

    private String generateGeminiPrompt(String taskName, String focus) {
        String promptTemplate = focus.equals("person") ?
                "You are an expert in crafting vivid, engaging image prompts. Your task is to transform a simple task name into " +
                        "a rich, immersive image description that illustrates a person performing the task in an enjoyable and inviting way. " +
                        "- Clearly depict a person actively engaged in the task. " +
                        "- Unless the task explicitly specifies a different gender, assume the person is a man. " +
                        "- Emphasize body language and facial expressions that convey enjoyment, focus, or relaxation. " +
                        "- Describe the environment in a way that makes the scene feel pleasant, warm, or inspiring. " +
                        "- Keep the description under 1000 characters. " +
                        "Task: \"" + taskName + "\" Image Description:" :
                "You are an expert in crafting vivid, engaging image prompts. Your task is to transform a simple task name into " +
                        "a rich, immersive image description that illustrates the primary object associated with the task in a visually appealing way. " +
                        "- Focus on the central object related to the task rather than a person performing it. " +
                        "- Highlight details that make the object visually interesting, such as texture, color, lighting, and any unique features. " +
                        "- Place the object in an environment that enhances its aesthetic appeal and purpose. " +
                        "- Keep the description under 1000 characters. " +
                        "Task: \"" + taskName + "\" Image Description:";

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

    private String extractImageUrlFromRecraftResponse(Map response) {
        try {
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (data != null && !data.isEmpty()) {
                Map<String, Object> firstImage = data.get(0);
                return (String) firstImage.get("url");
            }
            throw new RuntimeException("No image URL found in response");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract image URL from response: " + e.getMessage());
        }
    }

    private String generateRecraftImage(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + recraftApiKey);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("prompt", prompt);
        requestBody.put("style_id", styleId);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://external.api.recraft.ai/v1/images/generations",
                HttpMethod.POST,
                request,
                Map.class
        );

        return extractImageUrlFromRecraftResponse(response.getBody());
    }

    private String storeImage(String imageUrl, String storagePath) throws Exception {

        // Download image from URL
        URL url = new URL(imageUrl);
        InputStream imageStream = url.openStream();

        // Upload to Firebase Storage
        Blob blob = storageBucket.create(storagePath, imageStream, "image/jpeg");

        return blob.signUrl(365 * 100, TimeUnit.DAYS)
                .toString();
    }

    private void updateFirestore(String userId, String routineId, String taskId, String imageUrl) {
        String documentPath = String.format("users/%s/routines/%s/tasks/%s", userId, routineId, taskId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("image", imageUrl);

        firestore.document(documentPath).update(updates);
    }

    private String extractPromptFromGeminiResponse(Map response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> firstCandidate = candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return parts.get(0).get("text");
                }
            }
            throw new RuntimeException("No text found in Gemini response");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from Gemini response: " + e.getMessage());
        }
    }
}