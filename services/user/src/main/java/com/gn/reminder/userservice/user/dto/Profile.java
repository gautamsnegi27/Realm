package com.gn.reminder.userservice.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

}
