package com.sonuSaitring.sonuSaitringManagement.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                Map<String, String> errors = new LinkedHashMap<>();

                ex.getBindingResult()
                                .getFieldErrors()
                                .forEach(fieldError -> errors.put(
                                                fieldError.getField(),
                                                fieldError.getDefaultMessage()));

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Validation failed",
                                request.getRequestURI(),
                                errors);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
                        ConstraintViolationException ex,
                        HttpServletRequest request) {

                Map<String, String> errors = new LinkedHashMap<>();

                ex.getConstraintViolations().forEach(violation -> errors.put(
                                violation.getPropertyPath().toString(),
                                violation.getMessage()));

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Validation failed",
                                request.getRequestURI(),
                                errors);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
                        HttpMessageNotReadableException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Invalid request body.",
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ApiErrorResponse> handleMissingParameter(
                        MissingServletRequestParameterException ex,
                        HttpServletRequest request) {

                Map<String, String> errors = new LinkedHashMap<>();

                errors.put(
                                ex.getParameterName(),
                                "This parameter is required.");

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Missing required request parameter.",
                                request.getRequestURI(),
                                errors);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
                        MethodArgumentTypeMismatchException ex,
                        HttpServletRequest request) {

                Map<String, String> errors = new LinkedHashMap<>();

                errors.put(
                                ex.getName(),
                                "Invalid value.");

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Invalid request parameter.",
                                request.getRequestURI(),
                                errors);
        }

        @ExceptionHandler(DateTimeException.class)
        public ResponseEntity<ApiErrorResponse> handleDateTimeException(
                        DateTimeException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Invalid date or time value.",
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.NOT_FOUND,
                                ex.getMessage(),
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ApiErrorResponse> handleBadRequest(
                        BadRequestException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                ex.getMessage(),
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ApiErrorResponse> handleConflict(
                        ConflictException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.CONFLICT,
                                ex.getMessage(),
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ApiErrorResponse> handleUnauthorized(
                        UnauthorizedException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.UNAUTHORIZED,
                                ex.getMessage(),
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(ExternalServiceException.class)
        public ResponseEntity<ApiErrorResponse> handleExternalService(
                        ExternalServiceException ex,
                        HttpServletRequest request) {

                logger.error(
                                "External service failure on {}: {}",
                                request.getRequestURI(),
                                ex.getMessage(),
                                ex);

                return buildResponse(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "A required external service is currently unavailable.",
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
                        DataIntegrityViolationException ex,
                        HttpServletRequest request) {

                logger.error(
                                "Database integrity violation on {}",
                                request.getRequestURI(),
                                ex);

                return buildResponse(
                                HttpStatus.CONFLICT,
                                "The request conflicts with existing data.",
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(DataAccessException.class)
        public ResponseEntity<ApiErrorResponse> handleDataAccessException(
                        DataAccessException ex,
                        HttpServletRequest request) {

                logger.error(
                                "Database access failure on {}",
                                request.getRequestURI(),
                                ex);

                return buildResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "A database error occurred.",
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
                        Exception ex,
                        HttpServletRequest request) {

                logger.error(
                                "Unexpected backend exception on {}",
                                request.getRequestURI(),
                                ex);

                return buildResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "An unexpected error occurred.",
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
                        MaxUploadSizeExceededException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Uploaded file is too large.",
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(MultipartException.class)
        public ResponseEntity<ApiErrorResponse> handleMultipartException(
                        MultipartException ex,
                        HttpServletRequest request) {

                logger.warn(
                                "Multipart request failure on {}",
                                request.getRequestURI(),
                                ex);

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Invalid file upload request.",
                                request.getRequestURI(),
                                null);
        }

        private ResponseEntity<ApiErrorResponse> buildResponse(
                        HttpStatus status,
                        String message,
                        String path,
                        Map<String, String> errors) {

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                status.value(),
                                status.getReasonPhrase(),
                                message,
                                path,
                                errors);

                return ResponseEntity
                                .status(status)
                                .body(response);
        }
}