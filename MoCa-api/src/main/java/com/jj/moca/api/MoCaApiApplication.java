package com.jj.moca.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.jj.moca")
@EntityScan(basePackages = "com.jj.moca")
@EnableJpaRepositories(basePackages = "com.jj.moca")
public class MoCaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoCaApiApplication.class, args);
    }
}
