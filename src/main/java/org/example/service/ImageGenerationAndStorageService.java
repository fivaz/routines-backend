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
public class ImageGenerationAndStorageService {

    @Value("${gemini.api.key}")
    String geminiApiKey;

    @Value("${recraft.api.key}")
    String recraftApiKey;

    @Value("${recraft.style.id}")
    String styleId;

    final RestTemplate restTemplate;
    private final Firestore firestore;
    private final Bucket storageBucket;

    @Autowired
    public ImageGenerationAndStorageService(RestTemplate restTemplate, Firestore firestore,
                                            Bucket storageBucket) {
        this.restTemplate = restTemplate;
        this.firestore = firestore;
        this.storageBucket = storageBucket;
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

    String generateRecraftImage(String prompt) {
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

    String storeImage(String imageUrl, String storagePath) throws Exception {

        // Download image from URL
        URL url = new URL(imageUrl);
        InputStream imageStream = url.openStream();

        // Upload to Firebase Storage
        Blob blob = storageBucket.create(storagePath, imageStream, "image/jpeg");

        return blob.signUrl(365 * 100, TimeUnit.DAYS)
                .toString();
    }

    void updateFirestore(String path, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("image", imageUrl);

        firestore.document(path).update(updates);
    }

    String extractPromptFromGeminiResponse(Map response) {
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