package com.sonuSaitring.sonuSaitringManagement.common.exception;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> errors;
    private String traceId;
}