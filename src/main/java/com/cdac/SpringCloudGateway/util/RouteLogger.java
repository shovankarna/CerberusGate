package com.cdac.SpringCloudGateway.util;

import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
public class RouteLogger {

    private final RouteDefinitionLocator routeDefinitionLocator;

    public RouteLogger(RouteDefinitionLocator routeDefinitionLocator) {
        this.routeDefinitionLocator = routeDefinitionLocator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logLoadedRoutes() {
        routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .doOnNext(definitions -> {
                    System.out.println("🚀 Total Routes Loaded: " + definitions.size());
                    definitions.forEach(route -> {
                        System.out.println("➡ Route ID: " + route.getId());
                    });
                })
                .subscribe();
    }
}
