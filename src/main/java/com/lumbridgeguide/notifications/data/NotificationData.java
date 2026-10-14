package com.lumbridgeguide.notifications.data;

import lombok.Data;

import java.util.List;

@Data
public class NotificationData {
    private String id;
    private String type;
    private String title;
    private String description;
    private String link;
    private boolean read;
    private String createdAt;
    private List<NotificationActionData> actions;
    private String actionTaken;
}
