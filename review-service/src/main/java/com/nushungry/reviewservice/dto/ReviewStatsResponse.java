package com.nushungry.reviewservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsResponse {

    private Long stallId;
    private Double averageRating;
    private Long totalReviews;
    private Double averagePrice;
    private Long totalPriceReviews;
}
