package com.gn.reminder.userservice.application.user.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Builder(toBuilder = true)
public class Profile {
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String bio;
    private Location location;
    private Preferences preferences;
}
