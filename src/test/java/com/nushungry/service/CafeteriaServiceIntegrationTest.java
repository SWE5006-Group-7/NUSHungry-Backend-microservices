package com.nushungry.service;

import com.nushungry.model.Cafeteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CafeteriaServiceIntegrationTest {
    @Autowired
    private CafeteriaService cafeteriaService;
    @Test
    void saveCafeteria() throws Exception {
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.setName("Cafeteria");
        cafeteria.setDescription("Description");
        cafeteria.setLongitude(90);
        cafeteria.setLatitude(90);
        cafeteria.setLocation("Building 9201921");
        cafeteria.setNearestBusStop("Biz 2");
        assertThat(cafeteriaService.save(cafeteria)).isNotNull();
    }
    @Test
    void findAllCafeterias() {
        assertThat(cafeteriaService.findAll()).isNotEmpty();
    }
}
