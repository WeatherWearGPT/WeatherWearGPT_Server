package com.weatherweargpt.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "outfit_image")
@Data
public class OutfitImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    @ManyToOne
    @JoinColumn(name = "dialog_id", nullable = false)
    private Dialogue dialogue;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String generationPrompt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}