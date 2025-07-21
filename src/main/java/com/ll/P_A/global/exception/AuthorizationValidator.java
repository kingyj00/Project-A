package com.ll.P_A.global.exception;

import com.ll.P_A.security.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthorizationValidator {

    public void validateAuthor(User resourceOwner, Long currentUserId) {
        if (!resourceOwner.getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 접근할 수 있습니다.");
        }
    }
}