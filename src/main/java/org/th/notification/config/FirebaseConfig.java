package org.th.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    @Value("${firebase.credentials-json:}")
    private String credentialsJson;

    @PostConstruct
    public void initialize() {
        if (!firebaseEnabled) {
            log.info("Firebase is DISABLED (firebase.enabled=false). Worker will skip messages.");
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream credentialsStream = getCredentialsStream();
                if (credentialsStream == null) {
                    log.warn("❌ No Firebase credentials configured. Set firebase.credentials-json or firebase.credentials-path.");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase initialized successfully in Worker!");
            }
        } catch (IOException e) {
            log.error("❌ Failed to initialize Firebase in Worker.", e);
        }
    }

    private InputStream getCredentialsStream() throws IOException {
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            return new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        }

        if (credentialsPath != null && !credentialsPath.isBlank()) {
            return new FileInputStream(credentialsPath);
        }

        return null;
    }
}
