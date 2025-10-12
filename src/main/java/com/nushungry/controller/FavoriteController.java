package com.nushungry.controller;

import com.nushungry.model.Favorite;
import com.nushungry.model.Stall;
import com.nushungry.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    public ResponseEntity<Favorite> addFavorite(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        Long stallId = Long.valueOf(request.get("stallId").toString());

        Favorite favorite = favoriteService.addFavorite(userId.toString(), stallId);
        return ResponseEntity.ok(favorite);
    }

    @DeleteMapping
    public ResponseEntity<Void> removeFavorite(@RequestParam String userId, @RequestParam Long stallId) {
        favoriteService.removeFavorite(userId, stallId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Stall>> getUserFavorites(@PathVariable String userId) {
        List<Stall> favorites = favoriteService.getUserFavorites(userId);
        return ResponseEntity.ok(favorites);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @RequestParam String userId,
            @RequestParam Long stallId,
            Authentication authentication) {

        // 如果未认证，直接返回false
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("isFavorite", false));
        }

        boolean isFavorite = favoriteService.isFavorite(userId, stallId);
        return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
    }
}