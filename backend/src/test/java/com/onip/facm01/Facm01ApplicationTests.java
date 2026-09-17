package com.onip.facm01;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class Facm01ApplicationTests {

    @Test
    void contextLoads() {
        // Vérifie que le contexte Spring démarre correctement : sécurité (deux filtres
        // API JWT + dashboard session), JPA (entités/embeddables), et le seed de l'admin.
    }
}
