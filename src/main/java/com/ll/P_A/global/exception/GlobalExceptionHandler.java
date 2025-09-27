package com.ll.P_A.global.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ---------- 공통 에러 응답 빌더 ---------- */
    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return error(status, code, message, null);
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status, String code, String message, Map<String, Object> errors
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        if (errors != null && !errors.isEmpty()) {
            body.put("errors", errors);
        }
        return ResponseEntity.status(status).body(body);
    }

    /* ---------- 인증/인가 관련(통일) ---------- */

    // 인증 실패(미로그인/토큰 문제 등): 401
    @Hidden
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증이 필요합니다.");
    }

    // 계정 미존재: 401
    @Hidden
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        return error(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
    }

    // JWT 만료: 401
    @Hidden
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleJwtExpired(ExpiredJwtException ex) {
        return error(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다. 다시 로그인해주세요.");
    }

    // 인가 실패(권한 없음): 403
    @Hidden
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다.");
    }

    /* ---------- 요청/검증 오류 ---------- */

    // @Valid 본문 바인딩 오류: 400 + 필드 상세
    @Hidden
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> fields = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", fields);
    }

    // @Validated 파라미터/쿼리 검증 오류: 400
    @Hidden
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> fields = new HashMap<>();
        ex.getConstraintViolations().forEach(v ->
                fields.put(v.getPropertyPath().toString(), v.getMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", fields);
    }

    // JSON 파싱/본문 형식 오류: 400
    @Hidden
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "요청 본문 형식이 올바르지 않습니다.");
    }

    // 잘못된 인자/상태: 400
    @Hidden
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
    }

    @Hidden
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_STATE", ex.getMessage());
    }

    /* ---------- 그 외(서버 오류) ---------- */

    @Hidden
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "알 수 없는 오류가 발생했습니다.");
    }
}