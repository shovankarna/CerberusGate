package com.cdac.SpringCloudGateway.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TokenExtract {

    public static TokenDetails getTokenDetails(String token) {
        TokenDetails td = new TokenDetails();
        // System.out.println("TokenExtract ================>>>>> CALLLED");
        try {
            // Split the token
            String[] parts = token.split("\\.");

            // Decode the payload
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payload = new String(decoder.decode(parts[1]));

            // Parse the JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode payloadJson = mapper.readTree(payload);

            // Extract user ID
            String userId = payloadJson.get("sub").asText();
            td.setUserId(userId);

            // Extract roles
            JsonNode resourceAccess = payloadJson.path("resource_access");
            JsonNode reactClient = resourceAccess.path("reactclient");
            JsonNode rolesNode = reactClient.path("roles");

            List<String> roles = new ArrayList<>();
            if (rolesNode.isArray()) {
                for (JsonNode role : rolesNode) {
                    roles.add(role.asText());
                }
            }
            td.setRoles(roles);

        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception
        }

        return td;
    }
}