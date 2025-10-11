package com.gn.reminder.userservice.application.user.dto;

import lombok.*;

import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Builder(toBuilder = true)
public class Preferences {
    private List<String> categories;
    private NotificationPreferences notifications;
}
