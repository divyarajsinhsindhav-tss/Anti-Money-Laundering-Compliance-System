package org.tss.tm.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends ApplicationException {
    public BusinessRuleException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION", HttpStatus.BAD_REQUEST);
    }

    public BusinessRuleException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }
}
