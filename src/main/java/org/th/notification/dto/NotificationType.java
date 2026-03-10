package org.th.notification.dto;

public enum NotificationType {
    ORDER_STATUS,            // Order confirmed, preparing, ready, etc.
    NEW_REVIEW,              // Someone reviewed your shop
    RATING_UPDATE,            // Shop rating changed
    PROMOTION,               // Marketing/promotional
    SYSTEM,                  // System announcements
    SCHEDULED_ORDER_DUE,     // Alarm for shop to prepare scheduled order
    LOST_FOUND_WITNESS_ALERT, // Sent to users who were near a lost/found incident
    LOST_FOUND_OWNER_MATCH,   // Sent to LOST post owner when a matching FOUND post appears
    LOST_FOUND_CRITICAL_ALERT // Urgent broadcast for missing persons/passports
}
