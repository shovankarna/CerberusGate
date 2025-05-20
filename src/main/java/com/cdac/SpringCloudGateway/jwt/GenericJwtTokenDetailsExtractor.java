package com.cdac.SpringCloudGateway.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Generic extractor that pulls userId and roles from JWT claims based on
 * configurable paths.
 */
@Slf4j
@Component
public class GenericJwtTokenDetailsExtractor implements TokenDetailsExtractor {

    @Value("${jwt.claims.user-id:sub}")
    private String userIdClaim;

    @Value("${jwt.claims.roles:roles}")
    private String rolesClaim;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public TokenDetails extract(String token) {
        TokenDetails details = new TokenDetails();

        log.info("🔍 Extracting token details for JWT: {}", token.substring(0, 10) + "...");

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT token format. Expected 3 parts.");
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode payload = objectMapper.readTree(payloadJson);

            String userId = payload.path(userIdClaim).asText(null);
            if (userId == null) {
                throw new IllegalArgumentException("Missing user-id claim: " + userIdClaim);
            }
            details.setUserId(userId);

            JsonNode rolesNode = extractNestedNode(payload, rolesClaim);
            List<String> roles = new ArrayList<>();

            if (rolesNode.isArray()) {
                for (JsonNode role : rolesNode) {
                    log.debug("Extracted role: {}", role);
                    roles.add(role.asText().toUpperCase());
                }
            } else if (rolesNode.isTextual()) {
                roles.add(rolesNode.asText());
            } else {
                log.warn("No roles found under claim '{}'", rolesClaim);
            }

            details.setRoles(roles);
        } catch (Exception e) {
            log.error("Failed to extract token details from JWT", e);
            throw new RuntimeException("Invalid JWT token. Unable to extract claims.");
        }

        return details;
    }

    private JsonNode extractNestedNode(JsonNode node, String path) {
        String[] parts = path.split("\\.");
        JsonNode current = node;
        for (String part : parts) {
            if (current == null || current.isMissingNode())
                return null;
            current = current.path(part);
        }
        return current;
    }

}
