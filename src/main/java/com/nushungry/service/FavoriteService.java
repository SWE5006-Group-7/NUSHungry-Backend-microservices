package com.nushungry.service;

import com.nushungry.model.Favorite;
import com.nushungry.model.Stall;
import com.nushungry.repository.FavoriteRepository;
import com.nushungry.repository.StallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final StallRepository stallRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, StallRepository stallRepository) {
        this.favoriteRepository = favoriteRepository;
        this.stallRepository = stallRepository;
    }

    @Transactional
    public Favorite addFavorite(String userId, Long stallId) {
        if (favoriteRepository.existsByUserIdAndStallId(userId, stallId)) {
            throw new RuntimeException("Stall is already in favorites");
        }

        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new RuntimeException("Stall not found"));

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setStall(stall);

        return favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(String userId, Long stallId) {
        favoriteRepository.deleteByUserIdAndStallId(userId, stallId);
    }

    public List<Stall> getUserFavorites(String userId) {
        return favoriteRepository.findByUserId(userId)
                .stream()
                .map(Favorite::getStall)
                .collect(Collectors.toList());
    }

    public boolean isFavorite(String userId, Long stallId) {
        return favoriteRepository.existsByUserIdAndStallId(userId, stallId);
    }
}