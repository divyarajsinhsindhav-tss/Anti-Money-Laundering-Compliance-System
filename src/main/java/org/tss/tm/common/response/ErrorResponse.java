package org.tss.tm.common.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    private Map<String, String> fieldErrors;
    private List<String> details;
    public static ErrorResponse of(int status,
                                   String error,
                                   String message,
                                   String path,
                                   Map<String, String> fieldErrors,
                                   List<String> details) {
        return new ErrorResponse(
                status,
                error,
                message,
                path,
                Instant.now(),
                fieldErrors,
                details
        );
    }
    public static ErrorResponse of(int status,
                                   String error,
                                   String message,
                                   String path) {
        return new ErrorResponse(
                status,
                error,
                message,
                path,
                Instant.now(),
                null,
                null
        );
    }
    public static ErrorResponse of(int status,
                                   String error,
                                   String message,
                                   String path,
                                   Map<String, String> fieldErrors) {
        return new ErrorResponse(
                status,
                error,
                message,
                path,
                Instant.now(),
                fieldErrors,
                null
        );
    }
}