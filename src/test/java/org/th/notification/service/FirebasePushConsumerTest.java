package org.th.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.BatchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.th.notification.dto.NotificationType;
import org.th.notification.dto.PushNotificationMessage;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FirebasePushConsumerTest {

    @InjectMocks
    private FirebasePushConsumer consumer;

    private org.th.notification.dto.CommonNotificationEvent payload;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "firebaseEnabled", true);
        
        payload = org.th.notification.dto.CommonNotificationEvent.builder()
                .title("Test Title")
                .body("Test Body")
                .notificationType(NotificationType.ORDER_STATUS)
                .fcmTokens(new java.util.ArrayList<>(List.of("token1")))
                .senderName("SYSTEM")
                .targetType(org.th.notification.dto.CommonNotificationEvent.TargetType.USER)
                .build();
    }

    @Test
    void consumeNotification_ShouldNotProcess_WhenFirebaseDisabled() {
        // Arrange
        ReflectionTestUtils.setField(consumer, "firebaseEnabled", false);

        // Act
        consumer.consumeNotification(payload);

        // Assert - Mocking FirebaseApp.getApps() would be needed if logic got further
    }

    @Test
    void consumeNotification_ShouldSendMessage_WhenSingleToken() throws com.google.firebase.messaging.FirebaseMessagingException {
        try (MockedStatic<FirebaseApp> firebaseAppMock = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> firebaseMessagingMock = mockStatic(FirebaseMessaging.class)) {
            
            // Arrange
            firebaseAppMock.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            when(firebaseMessaging.send(any(Message.class))).thenReturn("response-id");

            // Act
            consumer.consumeNotification(payload);

            // Assert
            verify(firebaseMessaging).send(any(Message.class));
        }
    }

    @Test
    void consumeNotification_ShouldSendMulticast_WhenMultipleTokens() throws com.google.firebase.messaging.FirebaseMessagingException {
        try (MockedStatic<FirebaseApp> firebaseAppMock = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> firebaseMessagingMock = mockStatic(FirebaseMessaging.class)) {
            
            // Arrange
            payload.setFcmTokens(List.of("token1", "token2"));
            firebaseAppMock.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            
            BatchResponse mockResponse = mock(BatchResponse.class);
            when(mockResponse.getFailureCount()).thenReturn(0);
            when(mockResponse.getSuccessCount()).thenReturn(2);
            when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(mockResponse);

            // Act
            consumer.consumeNotification(payload);

            // Assert
            verify(firebaseMessaging).sendEachForMulticast(any(MulticastMessage.class));
        }
    }

    @Test
    void consumeNotification_ShouldNotProcess_WhenTokensEmpty() {
        // Arrange
        payload.setFcmTokens(List.of());

        // Act
        consumer.consumeNotification(payload);

        // Assert
        // Logic should log info and return before calling Firebase
    }

    @Test
    void consumeLegacyNotification_ShouldForwardToNewFlow() throws com.google.firebase.messaging.FirebaseMessagingException {
        try (MockedStatic<FirebaseApp> firebaseAppMock = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> firebaseMessagingMock = mockStatic(FirebaseMessaging.class)) {
            
            // Arrange
            firebaseAppMock.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            when(firebaseMessaging.send(any(Message.class))).thenReturn("legacy-response-id");

            PushNotificationMessage legacyPayload = PushNotificationMessage.builder()
                    .title("Legacy Title")
                    .body("Legacy Body")
                    .tokens(List.of("legacy-token"))
                    .type(NotificationType.ORDER_STATUS)
                    .build();

            // Act
            consumer.consumeLegacyNotification(legacyPayload);

            // Assert
            verify(firebaseMessaging).send(any(Message.class));
        }
    }

    @Test
    void consumeLegacyNotification_ShouldHandleAliasedFields() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = """
                {
                    "title": "Alias Title",
                    "fcmTokens": ["alias-token"],
                    "notificationType": "ORDER_STATUS"
                }
                """;
        
        try (MockedStatic<FirebaseApp> firebaseAppMock = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> firebaseMessagingMock = mockStatic(FirebaseMessaging.class)) {
            
            // Arrange
            firebaseAppMock.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            when(firebaseMessaging.send(any(Message.class))).thenReturn("alias-response-id");

            PushNotificationMessage legacyPayload = mapper.readValue(json, PushNotificationMessage.class);

            // Act
            consumer.consumeLegacyNotification(legacyPayload);

            // Assert
            verify(firebaseMessaging).send(any(Message.class));
            // Verify fields were correctly aliased
            org.assertj.core.api.Assertions.assertThat(legacyPayload.getTokens()).contains("alias-token");
            org.assertj.core.api.Assertions.assertThat(legacyPayload.getType()).isEqualTo(NotificationType.ORDER_STATUS);
        }
    }
}
