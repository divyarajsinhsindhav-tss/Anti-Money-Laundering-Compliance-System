package org.tss.tm.service.interfaces;

import org.tss.tm.dto.user.request.ChangePasswordRequest;
import org.tss.tm.dto.user.request.LoginRequest;
import org.tss.tm.dto.user.response.AuthResponse;
import org.tss.tm.dto.user.response.ChangePasswordResponse;

public interface AuthService {

    AuthResponse login(LoginRequest loginRequest);

    ChangePasswordResponse changePassword(ChangePasswordRequest changePasswordRequest, String email);
}
