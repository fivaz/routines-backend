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
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class TaskImageGenerationService extends ImageGenerationAndStorageService {


    @Autowired
    public TaskImageGenerationService(WebClient webClient, Firestore firestore, Bucket storageBucket) {
        super(webClient, firestore, storageBucket);
    }

    @Async
    public void generateAndStoreImage(String userId, String taskId, String routineId, String taskName, String focus) {
        String path = String.format("users/%s/routines/%s/tasks/%s", userId, routineId, taskId);

        CompletableFuture.runAsync(() -> {
            try {
                // Generate prompt using Gemini
                String prompt = generateGeminiPrompt(taskName, focus);

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