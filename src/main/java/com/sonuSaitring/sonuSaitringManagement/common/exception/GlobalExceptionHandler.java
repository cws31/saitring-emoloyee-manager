package com.sonuSaitring.sonuSaitringManagement.common.exception;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        private final Counter badRequestErrors;
        private final Counter unauthorizedErrors;
        private final Counter notFoundErrors;
        private final Counter conflictErrors;
        private final Counter tooManyRequestsErrors;
        private final Counter externalServiceErrors;
        private final Counter databaseErrors;
        private final Counter unexpectedErrors;

        private final Tracer tracer;

        public GlobalExceptionHandler(
                        MeterRegistry meterRegistry,
                        Tracer tracer) {

                this.tracer = tracer;

                this.badRequestErrors = Counter.builder(
                                "application.errors")
                                .tag("type", "bad_request")
                                .description("Number of bad request errors")
                                .register(meterRegistry);

                this.unauthorizedErrors = Counter.builder(
                                "application.errors")
                                .tag("type", "unauthorized")
                                .description("Number of unauthorized errors")
                                .register(meterRegistry);

                this.notFoundErrors = Counter.builder(
                                "application.errors")
                                .tag("type", "not_found")
                                .description("Number of resource not found errors")
                                .register(meterRegistry);

                this.conflictErrors = Counter.builder(
                                "application.errors")
                                .tag("type", "conflict")
                                .description("Number of conflict errors")
                                .register(meterRegistry);

                this.tooManyRequestsErrors = Counter.builder(
                                "application.errors")
                                .tag("type", "too_many_requests")
                                .description("Number of rate limit and cooldown errors")
                                .register(meterRegistry);

                this.externalServiceErrors = Counter.builder(
                                "application.errors")
                                .tag("type", "external_service")
                                .description("Number of external service errors")
                                .register(meterRegistry);

                this.databaseErrors = Counter.builder(
                                "application.errors")
                                .tag("type", "database")
                                .description("Number of database errors")
                                .register(meterRegistry);

                this.unexpectedErrors = Counter.builder(
                                "application.errors")
                                .tag("type", "unexpected")
                                .description("Number of unexpected application errors")
                                .register(meterRegistry);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                badRequestErrors.increment();

                Map<String, String> errors = new LinkedHashMap<>();

                ex.getBindingResult()
                                .getFieldErrors()
                                .forEach(fieldError -> errors.put(
                                                fieldError.getField(),
                                                fieldError.getDefaultMessage()));

                logger.warn(
                                "Request validation failed: method={}, path={}, fields={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                errors.keySet());

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

                badRequestErrors.increment();

                Map<String, String> errors = new LinkedHashMap<>();

                ex.getConstraintViolations()
                                .forEach(violation -> errors.put(
                                                violation.getPropertyPath().toString(),
                                                violation.getMessage()));

                logger.warn(
                                "Constraint validation failed: method={}, path={}",
                                request.getMethod(),
                                request.getRequestURI());

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

                badRequestErrors.increment();

                logger.warn(
                                "Malformed request body: method={}, path={}",
                                request.getMethod(),
                                request.getRequestURI());

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

                badRequestErrors.increment();

                Map<String, String> errors = new LinkedHashMap<>();

                errors.put(
                                ex.getParameterName(),
                                "This parameter is required.");

                logger.warn(
                                "Missing request parameter: method={}, path={}, parameter={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                ex.getParameterName());

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

                badRequestErrors.increment();

                Map<String, String> errors = new LinkedHashMap<>();

                errors.put(
                                ex.getName(),
                                "Invalid value.");

                logger.warn(
                                "Request parameter type mismatch: method={}, path={}, parameter={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                ex.getName());

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

                badRequestErrors.increment();

                logger.warn(
                                "Invalid date/time value: method={}, path={}",
                                request.getMethod(),
                                request.getRequestURI());

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

                notFoundErrors.increment();

                logger.warn(
                                "Resource not found: method={}, path={}, message={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                ex.getMessage());

                return buildResponse(
                                HttpStatus.NOT_FOUND,
                                ex.getMessage(),
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
                        NoResourceFoundException ex,
                        HttpServletRequest request) {

                notFoundErrors.increment();

                logger.warn(
                                "Endpoint/resource not found: method={}, path={}",
                                request.getMethod(),
                                request.getRequestURI());

                return buildResponse(
                                HttpStatus.NOT_FOUND,
                                "Resource not found.",
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ApiErrorResponse> handleBadRequest(
                        BadRequestException ex,
                        HttpServletRequest request) {

                badRequestErrors.increment();

                logger.warn(
                                "Bad request: method={}, path={}, message={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                ex.getMessage());

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

                conflictErrors.increment();

                logger.warn(
                                "Request conflict: method={}, path={}, message={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                ex.getMessage());

                return buildResponse(
                                HttpStatus.CONFLICT,
                                ex.getMessage(),
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(TooManyRequestsException.class)
        public ResponseEntity<ApiErrorResponse> handleTooManyRequests(
                        TooManyRequestsException ex,
                        HttpServletRequest request) {

                tooManyRequestsErrors.increment();

                logger.warn(
                                "Too many requests: method={}, path={}, message={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                ex.getMessage());

                return buildResponse(
                                HttpStatus.TOO_MANY_REQUESTS,
                                ex.getMessage(),
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ApiErrorResponse> handleUnauthorized(
                        UnauthorizedException ex,
                        HttpServletRequest request) {

                unauthorizedErrors.increment();

                logger.warn(
                                "Unauthorized request: method={}, path={}",
                                request.getMethod(),
                                request.getRequestURI());

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

                externalServiceErrors.increment();

                logger.error(
                                "External service failure: method={}, path={}, exception={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                ex.getClass().getSimpleName(),
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

                databaseErrors.increment();

                logger.error(
                                "Database integrity violation: method={}, path={}, exception={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                ex.getClass().getSimpleName(),
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

                databaseErrors.increment();

                logger.error(
                                "Database access failure: method={}, path={}, exception={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                ex.getClass().getSimpleName(),
                                ex);

                return buildResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "A database error occurred.",
                                request.getRequestURI(),
                                null);
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
                        MaxUploadSizeExceededException ex,
                        HttpServletRequest request) {

                badRequestErrors.increment();

                logger.warn(
                                "File upload rejected because size limit was exceeded: method={}, path={}",
                                request.getMethod(),
                                request.getRequestURI());

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

                badRequestErrors.increment();

                logger.warn(
                                "Multipart request failed: method={}, path={}, exception={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                ex.getClass().getSimpleName());

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "Invalid file upload request.",
                                request.getRequestURI(),
                                null);
        }

        /*
         * ------------------------------------------------------------
         * FALLBACK
         * ------------------------------------------------------------
         *
         * This handler is intentionally kept.
         *
         * It catches genuinely unexpected programming/infrastructure
         * failures that should not expose internal details to clients.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
                        Exception ex,
                        HttpServletRequest request) {

                unexpectedErrors.increment();

                logger.error(
                                "Unexpected backend exception: method={}, path={}, exception={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                ex.getClass().getSimpleName(),
                                ex);

                return buildResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "An unexpected error occurred.",
                                request.getRequestURI(),
                                null);
        }

        private ResponseEntity<ApiErrorResponse> buildResponse(
                        HttpStatus status,
                        String message,
                        String path,
                        Map<String, String> errors) {

                String traceId = null;

                if (tracer.currentSpan() != null) {
                        traceId = tracer.currentSpan()
                                        .context()
                                        .traceId();
                }

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                status.value(),
                                status.getReasonPhrase(),
                                message,
                                path,
                                errors,
                                traceId);

                return ResponseEntity
                                .status(status)
                                .body(response);
        }
}
