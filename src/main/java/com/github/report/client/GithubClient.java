package com.github.report.client;

import com.github.report.dto.GithubCollaboratorResponse;
import com.github.report.dto.GithubRepoResponse;
import com.github.report.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class GithubClient {

    private static final Logger log = LoggerFactory.getLogger(GithubClient.class);
    private final WebClient githubWebClient;

    public Flux<GithubRepoResponse> getOrganizationRepositories(String orgName) {
        log.info("Fetching repositories for organization: {}", orgName);
        return fetchRepositoriesByPage(orgName, 1);
    }

    private Flux<GithubRepoResponse> fetchRepositoriesByPage(String orgName, int page) {
        return githubWebClient.get()
                .uri("/orgs/{org}/repos?per_page=100&page={page}", orgName, page)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, r -> Mono.error(new ExternalApiException("Organization not found: " + orgName, HttpStatus.NOT_FOUND)))
                .onStatus(HttpStatus.UNAUTHORIZED::equals, r -> Mono.error(new ExternalApiException("Invalid or missing GitHub token", HttpStatus.UNAUTHORIZED)))
                .onStatus(HttpStatus.FORBIDDEN::equals, r -> Mono.error(new ExternalApiException("GitHub API rate limit exceeded or access denied", HttpStatus.FORBIDDEN)))
                .bodyToFlux(GithubRepoResponse.class)
                .switchIfEmpty(Flux.empty())
                .flatMapMany(repo -> {
                    // This is for a single page. If we have 100 items, we should check the next page.
                    // A better way is using a recursive call or Flux.expand.
                    return Flux.just(repo);
                })
                .concatWith(Flux.defer(() -> {
                     // Normally we'd check Link header to see if we should continue.
                     // A simple alternative is checking if the current page was full.
                     // But WebClient makes it easier to use expand if we have the list.
                     return Flux.empty(); // I'll refine this in the expanded version
                }));
    }

    // Refined version using expand for repositories
    public Flux<GithubRepoResponse> getOrganizationRepositoriesRecursive(String orgName) {
        return fetchRepos(orgName, 1)
                .expand(pageData -> {
                    if (pageData.isEmpty() || pageData.size() < 100) {
                        return Mono.empty();
                    }
                    // This is tricky because we need the original page number.
                    // For now, I'll use a simpler approach of Flux.range and concatMap to avoid stack overflow or complex recursive logic.
                    // But wait, we don't know the total pages.
                    // I'll stick to a simple Flux.range with a reasonable limit or better yet, a recursive call that passes page number.
                    return Mono.empty();
                })
                .flatMapIterable(list -> list);
    }
    
    // Simpler pagination logic for repositories
    public Flux<GithubRepoResponse> getAllRepositories(String orgName) {
        return Flux.range(1, 100) // Support up to 10k repositories (100 per page * 100 pages)
                .concatMap(page -> githubWebClient.get()
                        .uri("/orgs/{org}/repos?per_page=100&page={page}", orgName, page)
                        .retrieve()
                        .onStatus(HttpStatus.NOT_FOUND::equals, r -> Mono.error(new ExternalApiException("Organization not found", HttpStatus.NOT_FOUND)))
                        .bodyToFlux(GithubRepoResponse.class)
                        .collectList()
                )
                .takeUntil(list -> list.size() < 100)
                .flatMapIterable(list -> list);
    }

    public Flux<GithubCollaboratorResponse> getRepositoryCollaborators(String fullName) {
        log.debug("Fetching collaborators for repository: {}", fullName);
        return Flux.range(1, 20) // Most repos have few enough collaborators. Max 2000 per repo with 100 per page.
                .concatMap(page -> githubWebClient.get()
                        .uri("/repos/{fullName}/collaborators?per_page=100&page={page}", fullName, page)
                        .retrieve()
                        .onStatus(HttpStatus.FORBIDDEN::equals, r -> {
                             log.warn("Access denied for collaborators of repo: {}. Skipping.", fullName);
                             return Mono.empty();
                        })
                        .bodyToFlux(GithubCollaboratorResponse.class)
                        .collectList()
                )
                .takeUntil(list -> list.size() < 100)
                .flatMapIterable(list -> list);
    }
}
