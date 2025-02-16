package org.example.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class FirebaseService {
    private Firestore firestore;

    @PostConstruct
    public void initialize() {
        try {
            // Read the service account file into a byte array
            byte[] serviceAccountBytes = Files.readAllBytes(Paths.get("src/main/resources/firebase-credentials.json"));

            // Create a single GoogleCredentials instance
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountBytes));

            // Initialize Firebase options
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            FirebaseApp.initializeApp(options);

            // Initialize Firestore options
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(credentials)
                    .build();

            this.firestore = firestoreOptions.getService();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveToFirestore(String collection, String document, Object data) {
        firestore.collection(collection).document(document).set(data);
    }
}