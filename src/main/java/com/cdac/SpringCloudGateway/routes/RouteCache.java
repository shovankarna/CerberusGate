package com.cdac.SpringCloudGateway.routes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Caches resolved routes to reduce lookup time and improve performance.
 * Supports reactive route matching using RouteLocator predicates.
 */
@Slf4j
@Component
public class RouteCache {

    private final RouteLocator routeLocator;
    private final Map<String, Route> cache = new ConcurrentHashMap<>();
    private final AtomicLong lastRefreshTime = new AtomicLong(System.currentTimeMillis());

    // ✅ Injected via application.yml (in milliseconds)
    @Value("${gateway.route-cache-refresh-interval-ms:60000}")
    private long cacheRefreshIntervalMs;

    public RouteCache(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        refresh();
        log.info("Route cache initialized with {} routes.", cache.size());
    }

    public Mono<Route> getMatchingRoute(ServerWebExchange exchange) {
        // Refresh cache if stale
        if (System.currentTimeMillis() - lastRefreshTime.get() > cacheRefreshIntervalMs) {
            refresh();
        }

        // Try to match route from cache
        return Flux.fromIterable(cache.values())
                .filterWhen(route -> route.getPredicate().apply(exchange))
                .next();
    }

    private void refresh() {
        routeLocator.getRoutes()
                .collectList()
                .doOnNext(routes -> {
                    cache.clear();
                    routes.forEach(route -> cache.put(route.getId(), route));
                    lastRefreshTime.set(System.currentTimeMillis());
                    log.info("Route cache refreshed with {} routes.", cache.size());
                })
                .subscribe();
    }
}
