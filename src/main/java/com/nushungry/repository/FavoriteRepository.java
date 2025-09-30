package com.nushungry.repository;

import com.nushungry.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserId(String userId);
    Optional<Favorite> findByUserIdAndStallId(String userId, Long stallId);
    boolean existsByUserIdAndStallId(String userId, Long stallId);
    void deleteByUserIdAndStallId(String userId, Long stallId);
}