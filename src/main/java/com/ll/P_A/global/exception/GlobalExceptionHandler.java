package com.ll.P_A.global.exception;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 클라이언트가 잘못된 요청을 보냈을 때
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(error); // 400
    }

    // 인증은 되었지만 권한이 없을 때 (예: 관리자만 접근 가능)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "접근 권한이 없습니다.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error); // 403
    }

    // 인증 과정에서 사용자를 찾을 수 없을 때
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUsernameNotFound(UsernameNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "사용자를 찾을 수 없습니다.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error); // 401
    }

    // JWT가 만료되었을 때
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, String>> handleJwtExpired(ExpiredJwtException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "토큰이 만료되었습니다. 다시 로그인해주세요.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error); // 401
    }

    // 상태적으로 잘못된 흐름 (예: 이메일 인증 이미 완료)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error); // 400
    }

    // 그 외 모든 예외 (시스템 오류 등)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOther(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "알 수 없는 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error); // 500
    }
}