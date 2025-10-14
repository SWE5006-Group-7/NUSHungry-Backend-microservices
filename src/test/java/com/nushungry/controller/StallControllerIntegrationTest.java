package com.nushungry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.model.Stall;
import com.nushungry.repository.StallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class StallControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StallRepository stallRepository;

    @BeforeEach
    void cleanUp() {
        stallRepository.deleteAllInBatch();
    }

    @Test
    void givenValidStall_whenCreate_thenReturnCreatedStall() throws Exception {
        // 创建请求数据
        Stall stall = new Stall();
        stall.setName("New Stall");
        stall.setCuisineType("Chinese");
        stall.setHalalInfo("Halal");
        stall.setContact("12345678");
        stall.setAverageRating(4.5);
        stall.setReviewCount(10);
        stall.setAveragePrice(10.0);
        stall.setLatitude(1.3521);
        stall.setLongitude(103.8198);

        mockMvc.perform(post("/api/stalls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stall)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Stall"))
                .andExpect(jsonPath("$.cuisineType").value("Chinese"))
                .andExpect(jsonPath("$.halalInfo").value("Halal"));
    }
}