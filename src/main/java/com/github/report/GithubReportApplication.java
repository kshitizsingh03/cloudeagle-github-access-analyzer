package com.github.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GithubReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubReportApplication.class, args);
    }
}
