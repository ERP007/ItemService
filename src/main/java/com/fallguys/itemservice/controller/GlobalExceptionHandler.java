package com.fallguys.itemservice.controller;

import com.fallguys.itemservice.domain.exception.BusinessException;
import com.fallguys.itemservice.domain.exception.ItemErrorCode;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
        HttpStatus status = resolveStatus(ex);
        if (status.is5xxServerError()) {
            log.error("Business exception: {}", ex.getMessage(), ex);
        } else {
            log.warn("Business exception: {}", ex.getMessage());
        }

        return ResponseEntity.status(status).body(build(status, ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ProblemDetail> handleInvalidRequest(Exception ex) {
        log.warn("Invalid request: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(build(HttpStatus.BAD_REQUEST, ItemErrorCode.INVALID_REQUEST.getCode(), "잘못된 요청입니다."));
    }

    @ExceptionHandler({
            OptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<ProblemDetail> handleConcurrentModification(Exception ex) {
        log.warn("Concurrent modification: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(
                        HttpStatus.CONFLICT,
                        ItemErrorCode.CONCURRENT_MODIFICATION.getCode(),
                        ItemErrorCode.CONCURRENT_MODIFICATION.getDefaultMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unexpected exception", ex);

        return ResponseEntity.internalServerError()
                .body(build(HttpStatus.INTERNAL_SERVER_ERROR, ItemErrorCode.INTERNAL_ERROR.getCode(), "서버 내부 오류가 발생했습니다."));
    }

    private static HttpStatus resolveStatus(BusinessException ex) {
        return switch (ex.getErrorCode()) {
            case ITEM_NOT_FOUND, CATEGORY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_SKU, CONCURRENT_MODIFICATION -> HttpStatus.CONFLICT;
            case INVENTORY_SYNC_FAILED -> HttpStatus.BAD_GATEWAY;
            case INVENTORY_SYNC_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static ProblemDetail build(HttpStatus status, String errorCode, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("errorCode", errorCode);
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }
}
