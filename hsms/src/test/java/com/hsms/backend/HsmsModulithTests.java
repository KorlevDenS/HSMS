package com.hsms.backend;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class HsmsModulithTests {

    @Test
    void applicationModulesRespectDeclaredBoundaries() {
        ApplicationModules.of(HsmsApplication.class).verify();
    }
}
