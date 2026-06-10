package com.hsms.backend;

import com.hsms.backend.api_gateway.controller.AuthController;
import com.hsms.backend.api_gateway.controller.HarvesterController;
import com.hsms.backend.api_gateway.controller.InsuranceController;
import com.hsms.backend.api_gateway.controller.MissionController;
import com.hsms.backend.api_gateway.controller.OperationsController;
import com.hsms.backend.api_gateway.controller.ReportingController;
import com.hsms.backend.api_gateway.controller.RiskController;
import com.hsms.backend.api_gateway.controller.SecurityController;
import com.hsms.backend.api_gateway.controller.SimulationController;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.List;

class HsmsModulithTests {

    @Test
    void applicationModulesRespectDeclaredBoundaries() {
        ApplicationModules.of(HsmsApplication.class).verify();
    }

    @Test
    void restAdaptersRemainFocusedByDocumentedContour() {
        List<Class<?>> controllers = List.of(
                AuthController.class,
                OperationsController.class,
                HarvesterController.class,
                MissionController.class,
                RiskController.class,
                SecurityController.class,
                InsuranceController.class,
                SimulationController.class,
                ReportingController.class
        );

        controllers.forEach(controller -> {
            long endpointCount = List.of(controller.getDeclaredMethods()).stream()
                    .filter(HsmsModulithTests::isEndpoint)
                    .count();
            org.assertj.core.api.Assertions.assertThat(endpointCount)
                    .as(controller.getSimpleName() + " endpoint count")
                    .isBetween(1L, 14L);
            org.assertj.core.api.Assertions.assertThat(controller.getConstructors()[0].getParameterCount())
                    .as(controller.getSimpleName() + " dependency count")
                    .isLessThanOrEqualTo(2);
        });
    }

    private static boolean isEndpoint(Method method) {
        return method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PatchMapping.class);
    }
}
