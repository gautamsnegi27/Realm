package com.gn.reminder.userservice.application.user.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Builder(toBuilder = true)
public class SocialAccount {
    private String id;
    private boolean connected;
}
