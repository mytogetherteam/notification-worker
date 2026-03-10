package org.th.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.th.notification.config.RabbitMQConfig;
import org.th.notification.dto.PushNotificationMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FirebasePushConsumer {

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    /**
     * Listens to the RabbitMQ queue and executes the Firebase HTTP API calls.
     * This service is entirely decoupled from the database.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_FIREBASE_PUSH)
    public void consumeNotification(PushNotificationMessage payload) {

        if (!firebaseEnabled || FirebaseApp.getApps().isEmpty()) {
            log.warn("[Worker] Firebase is disabled or not initialized. Dropping message.");
            return;
        }

        List<String> tokens = payload.getTokens();
        if (tokens == null || tokens.isEmpty()) {
            log.warn("[Worker] No tokens provided in payload for '{}'. Dropping.", payload.getTitle());
            return;
        }

        try {
            Notification fcmNotification = Notification.builder()
                    .setTitle(payload.getTitle())
                    .setBody(payload.getBody())
                    .setImage(payload.getImageUrl())
                    .build();

            Map<String, String> data = new HashMap<>();
            data.put("type", payload.getType().name());
            if (payload.getReferenceId() != null) {
                data.put("referenceId", payload.getReferenceId().toString());
            }

            if (tokens.size() == 1) {
                Message message = Message.builder()
                        .setToken(tokens.get(0))
                        .setNotification(fcmNotification)
                        .putAllData(data)
                        .build();
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("[Worker] FCM push sent (1 device). Response: {}", response);
            } else {
                // Multicast handles up to 500 tokens
                int batchSize = 500;
                for (int i = 0; i < tokens.size(); i += batchSize) {
                    List<String> batch = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
                    MulticastMessage message = MulticastMessage.builder()
                            .addAllTokens(batch)
                            .setNotification(fcmNotification)
                            .putAllData(data)
                            .build();
                    var response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                    log.info("[Worker] FCM multicast batch sent. Success: {}, Failure: {}", 
                            response.getSuccessCount(), response.getFailureCount());
                }
            }

        } catch (Exception e) {
            log.error("[Worker] Failed to send FCM push for '{}'", payload.getTitle(), e);
        }
    }
}
