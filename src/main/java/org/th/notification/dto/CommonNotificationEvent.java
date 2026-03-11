package org.th.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Unified notification event payload shared between Core API and Worker.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonNotificationEvent implements Serializable {

    public enum TargetType {
        USER,
        SHOP,
        ADMIN,
        BROADCAST_USER,
        BROADCAST_SHOP
    }

    private TargetType targetType;
    private Long targetId;
    private String targetUsername;
    private String targetEmail;

    private String title;
    private String titleMm;
    private String body;
    private String bodyMm;

    private NotificationType notificationType;
    private Long referenceId;
    private String imageUrl;
    private String senderName;

    @Builder.Default
    private List<String> fcmTokens = new java.util.ArrayList<>(); // Added for Push Worker

    private Object payload; // Generic payload for WebSocket (e.g., OrderDTO)
}
