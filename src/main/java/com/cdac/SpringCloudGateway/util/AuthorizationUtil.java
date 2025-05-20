package com.cdac.SpringCloudGateway.util;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthorizationUtil {
    public static boolean hasAnyMatchingRole(List<String> userRoles, List<String> requiredRoles) {
        if (userRoles == null || requiredRoles == null)
            return false;

        for (String required : requiredRoles) {
            for (String actual : userRoles) {
                log.debug("🔍 Comparing required={} with actual={}", required, actual);
                if (required.equalsIgnoreCase(actual))
                    return true;
            }
        }
        return false;
    }

}
