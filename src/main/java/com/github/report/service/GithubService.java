package com.github.report.service;

import com.github.report.client.GithubClient;
import com.github.report.dto.GithubCollaboratorResponse;
import com.github.report.dto.GithubRepoResponse;
import com.github.report.dto.UserAccessReport;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GithubService {

    private static final Logger log = LoggerFactory.getLogger(GithubService.class);
    private final GithubClient githubClient;

    @Cacheable(value = "github-report", key = "#orgName")
    public Mono<UserAccessReport> generateAccessReport(String orgName) {
        log.info("Generating access report for organization: {}", orgName);

        // Map to store aggregated data: Username -> List of RepoDetail
        Map<String, List<UserAccessReport.RepoDetail>> userRepoMap = new ConcurrentHashMap<>();

        return githubClient.getAllRepositories(orgName)
                // Use flatMap for parallel processing of repositories. 
                // concurrency can be tuned, default is 256.
                .flatMap(repo -> {
                    log.debug("Processing repository: {}", repo.getFullName());
                    return githubClient.getRepositoryCollaborators(repo.getFullName())
                            .doOnNext(collaborator -> {
                                String username = collaborator.getLogin();
                                String permission = collaborator.getDerivedPermission();
                                
                                userRepoMap.computeIfAbsent(username, k -> Collections.synchronizedList(new ArrayList<>()))
                                        .add(UserAccessReport.RepoDetail.builder()
                                                .repoName(repo.getName())
                                                .permission(permission)
                                                .build());
                            })
                            .then(); // Only care about completion for each repo's collaborators
                }, 10) // Concurrency of 10 to be respectful of rate limits while still being fast
                .then(Mono.fromCallable(() -> {
                    log.info("Aggregating results for {} users", userRepoMap.size());
                    List<UserAccessReport.UserReport> userReports = userRepoMap.entrySet().stream()
                            .map(entry -> UserAccessReport.UserReport.builder()
                                    .username(entry.getKey())
                                    .repositories(entry.getValue())
                                    .build())
                            .sorted((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()))
                            .collect(Collectors.toList());

                    return UserAccessReport.builder()
                            .users(userReports)
                            .build();
                }));
    }
}
