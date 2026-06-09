package com.hsms.backend.readmodel;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HsmsModulesHealthIndicator implements HealthIndicator {

    private final HsmsModuleHealthService moduleHealthService;

    public HsmsModulesHealthIndicator(HsmsModuleHealthService moduleHealthService) {
        this.moduleHealthService = moduleHealthService;
    }

    @Override
    public Health health() {
        Map<String, Boolean> modules = moduleHealthService.snapshot();
        Health.Builder builder = modules.values().stream().allMatch(Boolean.TRUE::equals)
                ? Health.up()
                : Health.down();
        return builder.withDetail("modules", modules).build();
    }
}
