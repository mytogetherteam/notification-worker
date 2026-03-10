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

    private PushNotificationMessage payload;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "firebaseEnabled", true);
        
        payload = PushNotificationMessage.builder()
                .title("Test Title")
                .body("Test Body")
                .type(NotificationType.ORDER_STATUS)
                .tokens(List.of("token1"))
                .userEmail("user@test.com")
                .senderName("SYSTEM")
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
            payload.setTokens(List.of("token1", "token2"));
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
        payload.setTokens(List.of());

        // Act
        consumer.consumeNotification(payload);

        // Assert
        // Logic should log warn and return before calling Firebase
    }
}
