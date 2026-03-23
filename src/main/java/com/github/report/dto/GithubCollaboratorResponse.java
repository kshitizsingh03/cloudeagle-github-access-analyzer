package com.github.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubCollaboratorResponse {
    private String login;
    @JsonProperty("role_name")
    private String roleName;
    private Map<String, Boolean> permissions;

    public String getDerivedPermission() {
        if (roleName != null) return roleName;
        if (permissions != null) {
            if (Boolean.TRUE.equals(permissions.get("admin"))) return "admin";
            if (Boolean.TRUE.equals(permissions.get("push"))) return "write";
            if (Boolean.TRUE.equals(permissions.get("pull"))) return "read";
        }
        return "unknown";
    }
}
