package com.onip.facm01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// @EnableAsync : nécessaire pour AddressAiCheckService, dont l'appel IA de vérification d'adresse
// se fait dans un thread à part (ne doit jamais ralentir la synchro terrain).
@EnableAsync
@SpringBootApplication
public class Facm01Application {

    public static void main(String[] args) {
        SpringApplication.run(Facm01Application.class, args);
    }
}
