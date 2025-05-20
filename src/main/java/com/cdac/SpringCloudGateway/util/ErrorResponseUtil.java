package com.cdac.SpringCloudGateway.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility to send consistent JSON error responses across the gateway.
 */
public class ErrorResponseUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Mono<Void> respond(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            // Log and skip writing since the response is already sent
            return Mono.empty(); // or optionally: Mono.error(new IllegalStateException("Response already
                                 // committed"));
        }

        try {
            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("status", status.value());
            errorBody.put("error", status.getReasonPhrase());
            errorBody.put("message", message);
            errorBody.put("path", exchange.getRequest().getPath().value());
            errorBody.put("timestamp", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            byte[] bytes = objectMapper.writeValueAsBytes(errorBody);

            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().setContentLength(bytes.length);

            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            // Final fallback if JSON conversion fails
            if (!response.isCommitted()) {
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            }

            byte[] fallback = "{\"error\":\"Internal Server Error\"}".getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }
}
