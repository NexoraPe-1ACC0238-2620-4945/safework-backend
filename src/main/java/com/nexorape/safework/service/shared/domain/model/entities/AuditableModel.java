package com.nexorape.safework.service.shared.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
/*
  Base auditable model for entities.
  Provides common id, createdAt and updatedAt fields handled by Spring Data JPA auditing.
  */
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class AuditableModel {
    // Primary key
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    // Creation timestamp
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    @Getter
    // Last modification timestamp
    @LastModifiedDate
    @Column(nullable = false)
    private Date updatedAt;
}
