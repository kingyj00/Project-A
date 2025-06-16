package com.ll.P_A.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmailVerificationToken(String token);
    Optional<User> findByEmail(String email);

    // DTO 기반 요약 정보만 조회
    @Query("SELECT u.id AS id, u.username AS username, u.email AS email FROM User u")
    List<UserSummary> findAllUserSummaries();
}