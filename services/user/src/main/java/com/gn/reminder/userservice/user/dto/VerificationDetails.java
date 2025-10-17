package com.gn.reminder.userservice.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationDetails {
    private boolean emailVerified;

    private boolean phoneVerified;

    private boolean identityVerified;
}

