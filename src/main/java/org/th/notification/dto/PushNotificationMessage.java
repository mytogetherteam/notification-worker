package org.th.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationMessage implements Serializable {

    private boolean isBroadcast;
    @com.fasterxml.jackson.annotation.JsonAlias("fcmTokens")
    private List<String> tokens;

    private String title;
    private String titleMm;
    private String body;
    private String bodyMm;

    @com.fasterxml.jackson.annotation.JsonAlias("notificationType")
    private NotificationType type;
    private Long referenceId;
    private String imageUrl;

    private String userEmail;
    private String senderName;
}
