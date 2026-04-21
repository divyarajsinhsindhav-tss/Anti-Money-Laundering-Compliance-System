package org.tss.tm.common.response;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private int status;
    private String message;
    private String path;
    private T data;


    public static <T> ApiResponse<T> of(
            HttpStatus status,
            String message,
            String path,
            T data
    ) {
        return new ApiResponse<>(
                LocalDateTime.now(),
                status.value(),
                message,
                path,
                data
        );
    }
}