package com.example.p1;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import jakarta.annotation.PostConstruct; // 임포트
import lombok.RequiredArgsConstructor; // 임포트

@SpringBootApplication
@EnableJpaAuditing
@RequiredArgsConstructor // LineupPlayerMigrationService 주입을 위해 추가
public class P1Application {

    public static void main(String[] args) {
        SpringApplication.run(P1Application.class, args);
    }

}