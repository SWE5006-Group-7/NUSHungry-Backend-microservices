package com.nushungry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.model.Stall;
import com.nushungry.service.StallService;
import com.nushungry.service.ImageService;
import com.nushungry.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = StallController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class StallControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StallService stallService;

    @MockBean
    private ImageService imageService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private com.nushungry.service.UserService userService;

    @MockBean
    private com.nushungry.service.SearchHistoryService searchHistoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenStalls_whenGetAll_thenReturnJsonArray() throws Exception {
        Stall stall1 = new Stall();
        stall1.setId(1L);
        stall1.setName("Stall 1");

        Stall stall2 = new Stall();
        stall2.setId(2L);
        stall2.setName("Stall 2");

        when(stallService.findAll()).thenReturn(Arrays.asList(stall1, stall2));

        mockMvc.perform(get("/api/stalls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Stall 1"))
                .andExpect(jsonPath("$[1].name").value("Stall 2"));

        verify(stallService).findAll();
    }

    @Test
    void givenStallId_whenGetById_thenReturnStall() throws Exception {
        Stall stall = new Stall();
        stall.setId(1L);
        stall.setName("Test Stall");

        when(stallService.findById(1L)).thenReturn(Optional.of(stall));

        mockMvc.perform(get("/api/stalls/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Stall"));

        verify(stallService).findById(1L);
    }

    @Test
    void givenInvalidId_whenGetById_thenReturn404() throws Exception {
        when(stallService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/stalls/99"))
                .andExpect(status().isNotFound());

        verify(stallService).findById(99L);
    }

    @Test
    void givenValidStall_whenCreate_thenReturnCreatedStall() throws Exception {
        // 使用DTO创建请求数据，避免JPA实体序列化问题
        StallCreateDTO requestDTO = new StallCreateDTO();
        requestDTO.setId(1L);
        requestDTO.setName("New Stall");
        requestDTO.setCuisineType("Chinese");
        requestDTO.setHalalInfo("Halal");
        requestDTO.setContact("12345678");
        requestDTO.setAverageRating(4.5);
        requestDTO.setReviewCount(10);
        requestDTO.setAveragePrice(10.0);
        requestDTO.setLatitude(1.3521);
        requestDTO.setLongitude(103.8198);

        // 创建模拟的保存后的Stall对象
        Stall savedStall = new Stall();
        savedStall.setId(1L);
        savedStall.setName("New Stall");
        savedStall.setCuisineType("Chinese");
        savedStall.setHalalInfo("Halal");
        savedStall.setContact("12345678");
        savedStall.setAverageRating(4.5);
        savedStall.setReviewCount(10);
        savedStall.setAveragePrice(10.0);
        savedStall.setLatitude(1.3521);
        savedStall.setLongitude(103.8198);

        when(stallService.save(any(Stall.class))).thenReturn(savedStall);

        mockMvc.perform(post("/api/stalls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Stall"));

        verify(stallService).save(any(Stall.class));
    }
}
