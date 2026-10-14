package com.lumbridgeguide.notifications.data;

import lombok.Data;

import java.util.List;

@Data
public class NotificationPageData {
    private List<NotificationData> notifications;
    private long totalElements;
}
