package com.gn.reminder.userservice.application.user.dto;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Builder(toBuilder = true)
@Document
public class Reputation {
    private int score;
    private String level;
    private List<String> badges;
}
