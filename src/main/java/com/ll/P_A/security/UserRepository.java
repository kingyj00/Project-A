package com.ll.P_A.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 및 인증용
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // 이메일 인증 토큰은 '원문'이 아니라 '해시'로 조회
    Optional<User> findByEmailVerificationTokenHash(String tokenHash);

    // 관리자 전용 사용자 요약 조회
    @Query("SELECT u.id AS id, u.username AS username, u.email AS email FROM User u")
    List<UserSummary> findAllUserSummaries();
}