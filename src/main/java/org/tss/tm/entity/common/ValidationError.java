package org.tss.tm.entity.common;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ValidationError {
    private int rowNumber;
    private String column;
    private String message;
    private String value;
}