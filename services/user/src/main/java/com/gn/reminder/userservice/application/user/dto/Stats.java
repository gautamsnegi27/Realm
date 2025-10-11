package com.gn.reminder.userservice.application.user.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Builder(toBuilder = true)
public class Stats {
    private int totalReviews;
    private int helpfulVotes;
    private int followersCount;
    private int followingCount;
}
