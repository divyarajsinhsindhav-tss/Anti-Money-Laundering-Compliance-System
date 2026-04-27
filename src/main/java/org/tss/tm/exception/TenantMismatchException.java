package org.tss.tm.exception;

import org.springframework.http.HttpStatus;

public class TenantMismatchException extends ApplicationException {
    public TenantMismatchException(String message) {
        super(message, "TENANT_MISMATCH", HttpStatus.FORBIDDEN);
    }
}
