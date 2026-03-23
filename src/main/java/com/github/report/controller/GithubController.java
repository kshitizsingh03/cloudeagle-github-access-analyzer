package com.github.report.controller;

import com.github.report.dto.UserAccessReport;
import com.github.report.service.GithubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Tag(name = "GitHub Report API", description = "Endpoints for generating GitHub organization access reports")
public class GithubController {

    private static final Logger log = LoggerFactory.getLogger(GithubController.class);
    private final GithubService githubService;

    @Operation(summary = "Generate user access report for a GitHub organization",
               description = "Fetches all repositories of an organization and maps users to repositories with their permission levels.")
    @GetMapping("/access-report")
    public Mono<ResponseEntity<UserAccessReport>> getAccessReport(@RequestParam("org") String orgName) {
        log.info("Received request for organization report: {}", orgName);
        
        if (orgName == null || orgName.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return githubService.generateAccessReport(orgName)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Failed to generate report for org: {}. Message: {}", orgName, e.getMessage()))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }
}
