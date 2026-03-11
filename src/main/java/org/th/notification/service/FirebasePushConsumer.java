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
import org.slf4j.MDC;
import java.util.UUID;

@Service
@Slf4j
public class FirebasePushConsumer {

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    /**
     * Listens to the RabbitMQ queue and executes the Firebase HTTP API calls.
     * This service is entirely decoupled from the database.
     */
    /**
     * Listens to the unified notifications queue and executes Firebase Push.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATIONS_PUSH)
    public void consumeNotification(org.th.notification.dto.CommonNotificationEvent event) {
        if (event == null) {
            log.error("[Worker] Received null notification event. Skipping.");
            return;
        }

        try {
            // Populate MDC for BetterStack filtering
            MDC.put("requestId", "push-" + UUID.randomUUID().toString().substring(0, 8));
            MDC.put("apiTag", "PUSH_WORKER");
            MDC.put("targetType", event.getTargetType() != null ? event.getTargetType().name() : "UNKNOWN");
            MDC.put("userEmail", event.getTargetEmail() != null ? event.getTargetEmail() : "broadcast");
            MDC.put("senderName", event.getSenderName() != null ? event.getSenderName() : "SYSTEM");

            if (!firebaseEnabled || FirebaseApp.getApps().isEmpty()) {
                log.warn("[Worker] Firebase is disabled or not initialized. Dropping message for referencedId: {}", event.getReferenceId());
                return;
            }

            List<String> tokens = event.getFcmTokens();
            if (tokens == null || tokens.isEmpty()) {
                log.info("[Worker] No FCM tokens for event type {}. Skipping push. ReferenceId: {}", 
                        event.getNotificationType(), event.getReferenceId());
                return;
            }

            // Remove duplicate tokens to avoid double-sends and save resources
            tokens = tokens.stream().filter(java.util.Objects::nonNull).distinct().toList();
            if (tokens.isEmpty()) {
                log.info("[Worker] All tokens were null after filtering. Skipping push.");
                return;
            }
            
            log.info("[Worker] Processing Push: Title='{}', Type={}, Sender={}, TargetDevices={}", 
                    event.getTitle(), event.getNotificationType(), event.getSenderName(), tokens.size());

            try {
                Notification fcmNotification = Notification.builder()
                        .setTitle(event.getTitle())
                        .setBody(event.getBody())
                        .setImage(event.getImageUrl())
                        .build();

                Map<String, String> data = new HashMap<>();
                data.put("type", event.getNotificationType() != null ? event.getNotificationType().name() : "DEFAULT");
                if (event.getReferenceId() != null) {
                    data.put("referenceId", event.getReferenceId().toString());
                }

                if (tokens.size() == 1) {
                    Message message = Message.builder()
                            .setToken(tokens.get(0))
                            .setNotification(fcmNotification)
                            .putAllData(data)
                            .build();
                    String response = FirebaseMessaging.getInstance().send(message);
                    log.info("[Worker] FCM push sent successfully (1 device). Response: {}. Payload: {}", response, data);
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
                        
                        if (response.getFailureCount() > 0) {
                            log.warn("[Worker] FCM multicast batch completed with failures. Success: {}, Failure: {}. Payload: {}", 
                                     response.getSuccessCount(), response.getFailureCount(), data);
                        } else {
                            log.info("[Worker] FCM multicast batch sent entirely successfully. Success: {}, Failure: 0. Payload: {}", 
                                     response.getSuccessCount(), data);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[Worker] Failed to send FCM push for '{}'. Error: {}", event.getTitle(), e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("[Worker] Global consumer error: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Legacy listener for backward compatibility during transition.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_FIREBASE_PUSH)
    public void consumeLegacyNotification(PushNotificationMessage payload) {
        if (payload == null) {
            log.error("[Worker] Received null legacy push payload. Skipping.");
            return;
        }

        log.info("[Worker] Received legacy push message. Converting to unified flow... Title: {}", payload.getTitle());
        
        // Convert PushNotificationMessage to CommonNotificationEvent
        org.th.notification.dto.CommonNotificationEvent event = org.th.notification.dto.CommonNotificationEvent.builder()
                .title(payload.getTitle())
                .titleMm(payload.getTitleMm())
                .body(payload.getBody())
                .bodyMm(payload.getBodyMm())
                .notificationType(payload.getType())
                .referenceId(payload.getReferenceId())
                .imageUrl(payload.getImageUrl())
                .senderName(payload.getSenderName())
                .targetEmail(payload.getUserEmail())
                .fcmTokens(payload.getTokens())
                .targetType(payload.isBroadcast() ? 
                        org.th.notification.dto.CommonNotificationEvent.TargetType.BROADCAST_USER : 
                        org.th.notification.dto.CommonNotificationEvent.TargetType.USER)
                .build();
        
        consumeNotification(event);
    }
}
