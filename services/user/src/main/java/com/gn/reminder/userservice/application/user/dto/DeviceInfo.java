package com.gn.reminder.userservice.application.user.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Builder(toBuilder = true)
public class DeviceInfo {
    private String userAgent;
    private String ipAddress;
    private String deviceType;
}
