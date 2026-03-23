package com.github.report.service;

import com.github.report.client.GithubClient;
import com.github.report.dto.GithubCollaboratorResponse;
import com.github.report.dto.GithubRepoResponse;
import com.github.report.dto.UserAccessReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class GithubServiceTest {

    @Mock
    private GithubClient githubClient;

    @InjectMocks
    private GithubService githubService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateAccessReport_Successful() {
        // Arrange
        String orgName = "test-org";
        String repo1 = "repo1";
        String repo2 = "repo2";
        String user1 = "alice";
        String user2 = "bob";

        GithubRepoResponse r1 = GithubRepoResponse.builder().name(repo1).fullName(orgName + "/" + repo1).build();
        GithubRepoResponse r2 = GithubRepoResponse.builder().name(repo2).fullName(orgName + "/" + repo2).build();

        GithubCollaboratorResponse c1 = GithubCollaboratorResponse.builder().login(user1).permissions(Map.of("admin", true)).build();
        GithubCollaboratorResponse c2 = GithubCollaboratorResponse.builder().login(user2).permissions(Map.of("pull", true)).build();

        when(githubClient.getAllRepositories(orgName)).thenReturn(Flux.just(r1, r2));
        when(githubClient.getRepositoryCollaborators(orgName + "/" + repo1)).thenReturn(Flux.just(c1));
        when(githubClient.getRepositoryCollaborators(orgName + "/" + repo2)).thenReturn(Flux.just(c1, c2));

        // Act & Assert
        StepVerifier.create(githubService.generateAccessReport(orgName))
                .assertNext(report -> {
                    assertNotNull(report);
                    assertEquals(2, report.getUsers().size());
                    
                    UserAccessReport.UserReport aliceReport = report.getUsers().stream()
                            .filter(u -> u.getUsername().equals(user1))
                            .findFirst().orElseThrow();
                    assertEquals(2, aliceReport.getRepositories().size());
                    assertTrue(aliceReport.getRepositories().stream().anyMatch(r -> r.getRepoName().equals(repo1) && r.getPermission().equals("admin")));
                    assertTrue(aliceReport.getRepositories().stream().anyMatch(r -> r.getRepoName().equals(repo2) && r.getPermission().equals("admin")));

                    UserAccessReport.UserReport bobReport = report.getUsers().stream()
                            .filter(u -> u.getUsername().equals(user2))
                            .findFirst().orElseThrow();
                    assertEquals(1, bobReport.getRepositories().size());
                    assertEquals("read", bobReport.getRepositories().get(0).getPermission());
                })
                .verifyComplete();
    }
}
