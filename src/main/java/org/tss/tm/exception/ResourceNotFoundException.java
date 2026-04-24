package org.tss.tm.exception;


import org.springframework.http.HttpStatus;
import org.tss.tm.exception.ApplicationException;

public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource+" not found "+identifier,"RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
