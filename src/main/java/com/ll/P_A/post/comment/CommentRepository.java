package com.ll.P_A.post.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List<CommentEntity> findByPostIdOrderByCreatedAtAsc(Long postId);
}