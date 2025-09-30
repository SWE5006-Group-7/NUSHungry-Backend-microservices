package com.nushungry.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "uploaded_by")
    private String uploadedBy; // 可以存储用户ID或用户名

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafeteria_id")
    @JsonBackReference("cafeteria-images")
    private Cafeteria cafeteria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stall_id")
    @JsonBackReference("stall-images")
    private Stall stall;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}