package org.tss.tm.dto.user.request;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;
}
