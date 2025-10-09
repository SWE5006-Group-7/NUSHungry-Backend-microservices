package com.nushungry.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String author; // Username for backward compatibility
    private double rating; // e.g., 1 to 5 (changed to double to allow half stars)
    private String comment;

    @Column(columnDefinition = "boolean default false")
    private boolean processed = false; // For tracking if a complaint/low rating has been handled

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stall_id")
    @JsonBackReference
    private Stall stall;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}