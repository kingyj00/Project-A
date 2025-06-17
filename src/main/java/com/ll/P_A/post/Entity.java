package com.ll.P_A.post;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Lob;

import java.time.LocalDateTime;

@Entity
public class Post {
    @Id @GeneratedValue
    private Long id;

    private String title;

    @Lob
    private String content;

    private String author;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}