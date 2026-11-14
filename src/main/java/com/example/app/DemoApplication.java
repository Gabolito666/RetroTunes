package com.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.example.app", "com.crud_relacional.com"})
@EntityScan(basePackages = {"com.crud_relacional.com.entidades"})
@EnableJpaRepositories(basePackages = {"com.crud_relacional.com.repositorio"})
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
