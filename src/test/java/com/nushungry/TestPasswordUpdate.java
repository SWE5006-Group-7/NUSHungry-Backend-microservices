package com.nushungry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class TestPasswordUpdate {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TestPasswordUpdate.class, args);

        BCryptPasswordEncoder encoder = context.getBean(BCryptPasswordEncoder.class);
        String password = "admin123";
        String encodedPassword = encoder.encode(password);

        System.out.println("=================================");
        System.out.println("Password: " + password);
        System.out.println("Encoded: " + encodedPassword);
        System.out.println("=================================");
        System.out.println("SQL to update admin password:");
        System.out.println("UPDATE users SET password='" + encodedPassword + "' WHERE username='admin';");
        System.out.println("=================================");

        // Test existing hash
        String existingHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
        System.out.println("Does 'admin123' match existing hash? " + encoder.matches("admin123", existingHash));
        System.out.println("Does 'password' match existing hash? " + encoder.matches("password", existingHash));

        context.close();
        System.exit(0);
    }
}