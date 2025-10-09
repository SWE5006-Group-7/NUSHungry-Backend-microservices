package com.nushungry.repository;

import com.nushungry.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByStallId(Long stallId);

    /**
     * 统计指定时间之后创建的评价数量
     */
    long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 获取所有评价的平均评分
     */
    @Query("SELECT AVG(r.rating) FROM Review r")
    Double getAverageRating();

    /**
     * 统计指定时间之前创建的评价数量
     */
    long countByCreatedAtBefore(LocalDateTime dateTime);

    /**
     * 统计指定时间范围内创建的评价数量
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 统计评分低于指定值且未处理的评价数量
     */
    long countByRatingLessThanAndProcessedFalse(Double rating);

    /**
     * 统计评分低于指定值的评价数量
     */
    long countByRatingLessThan(Double rating);
}