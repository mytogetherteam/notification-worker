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
    private List<String> tokens;

    private String title;
    private String titleMm;
    private String body;
    private String bodyMm;

    private NotificationType type;
    private Long referenceId;
    private String imageUrl;
}
