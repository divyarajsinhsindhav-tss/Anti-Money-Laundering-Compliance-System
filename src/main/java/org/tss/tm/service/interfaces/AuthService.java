package org.tss.tm.service.interfaces;

import org.tss.tm.dto.user.request.LoginRequest;
import org.tss.tm.dto.user.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest loginRequest);
}
