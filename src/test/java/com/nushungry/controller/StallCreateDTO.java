package com.nushungry.controller;

import lombok.Data;

@Data
public class StallCreateDTO {
    private Long id;
    private String name;
    private String cuisineType;
    private String halalInfo;
    private String contact;
    private Double averageRating;
    private Integer reviewCount;
    private Double averagePrice;
    private Double latitude;
    private Double longitude;
}