package com.example.knowledgebase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PrivateKnowledgeBaseApplicationTests {

    @Test
    @DisplayName("Application context should load and initialize database schema successfully")
    void contextLoads() {
        // Verifies Spring Context initializes, JPA connects to PostgreSQL,
        // and all beans are configured properly without circular dependencies.
    }
}
