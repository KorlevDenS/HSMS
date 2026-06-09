package com.hsms.backend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HsmsNfrSmokeTests extends H2IntegrationTestBase {

    @LocalServerPort
    int port;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    @Test
    void apiAndRiskPathStayWithinDocumentedNfrLimits() {
        Timed<Map<String, Object>> bootstrap = timed(() -> getJson("management", "/bootstrap"));
        assertThat(bootstrap.elapsed()).isLessThan(Duration.ofSeconds(2));
        assertThat(bootstrap.value()).containsKey("dashboard");

        Map<String, Object> harvester = post("dispatcher", "/harvesters", Map.of(
                "name", "HV-NFR-RISK",
                "type", "STANDARD",
                "status", "READY",
                "noiseLevel", 0.2,
                "capacity", 100
        ));
        Map<String, Object> crew = post("dispatcher", "/crews", Map.of(
                "name", "Экипаж NFR риск",
                "status", "READY",
                "contactChannel", "nfr-risk",
                "memberCount", 4,
                "assignedLogin", "crew"
        ));

        Map<String, Object> created = post("dispatcher", "/missions", Map.of(
                "title", "NFR smoke рейс",
                "zoneId", 1,
                "harvesterId", id(harvester),
                "crewId", id(crew),
                "plannedStart", Instant.now().plusSeconds(3600).toString(),
                "plannedEnd", Instant.now().plusSeconds(7200).toString(),
                "route", List.of(
                        Map.of("seqNo", 1, "lat", 24.42, "lon", 54.20),
                        Map.of("seqNo", 2, "lat", 24.50, "lon", 54.30)
                )
        ));

        long missionId = ((Number) created.get("id")).longValue();
        Timed<Map<String, Object>> risk = timed(() -> post("dispatcher", "/missions/" + missionId + "/risk-assessments", Map.of()));
        assertThat(risk.elapsed()).isLessThan(Duration.ofSeconds(3));
        assertThat((Integer) risk.value().get("riskScore")).isBetween(0, 100);
    }

    @Test
    void telemetryPostPathAcceptsDocumentedBurstWithoutErrors() {
        Map<String, Object> harvester = post("dispatcher", "/harvesters", Map.of(
                "name", "HV-NFR-TEL",
                "type", "STANDARD",
                "status", "READY",
                "noiseLevel", 0.2,
                "capacity", 100
        ));
        Map<String, Object> crew = post("dispatcher", "/crews", Map.of(
                "name", "Экипаж NFR телеметрия",
                "status", "READY",
                "contactChannel", "nfr-telemetry",
                "memberCount", 4,
                "assignedLogin", "crew"
        ));
        Map<String, Object> created = post("dispatcher", "/missions", Map.of(
                "title", "NFR telemetry burst рейс",
                "zoneId", 1,
                "harvesterId", id(harvester),
                "crewId", id(crew),
                "plannedStart", Instant.now().plusSeconds(3600).toString(),
                "plannedEnd", Instant.now().plusSeconds(7200).toString(),
                "route", List.of(
                        Map.of("seqNo", 1, "lat", 24.42, "lon", 54.20),
                        Map.of("seqNo", 2, "lat", 24.50, "lon", 54.30)
                )
        ));
        long missionId = id(created);
        post("dispatcher", "/missions/" + missionId + "/risk-assessments", Map.of());
        post("dispatcher", "/missions/" + missionId + "/launch", Map.of(
                "confirmWarning", true,
                "reason", "NFR проверка телеметрии"
        ));

        ExecutorService executor = Executors.newFixedThreadPool(32);
        Instant startedAt = Instant.now();
        try {
            List<CompletableFuture<Integer>> futures = IntStream.range(0, 200)
                    .mapToObj(index -> CompletableFuture.supplyAsync(() -> status("crew", "POST", "/missions/" + missionId + "/telemetry", Map.of(
                            "externalEventId", "nfr-tel-" + missionId + "-" + index,
                            "eventTime", Instant.now().plusMillis(index).toString(),
                            "lat", 24.42 + index * 0.0001,
                            "lon", 54.20 + index * 0.0001,
                            "equipmentStatus", "NORMAL"
                    )), executor))
                    .toList();
            List<Integer> statuses = futures.stream().map(CompletableFuture::join).toList();
            Duration elapsed = Duration.between(startedAt, Instant.now());
            assertThat(statuses).allSatisfy(status -> assertThat(status).isBetween(200, 299));
            assertThat(elapsed).isLessThan(Duration.ofSeconds(10));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void riskModelSanityCoversRangeAndMonotonicity() {
        Instant plannedStart = Instant.now().plusSeconds(3600);
        Map<String, Object> quietHarvester = post("dispatcher", "/harvesters", Map.of(
                "name", "HV-NFR-QUIET",
                "type", "LIGHT",
                "status", "READY",
                "noiseLevel", 0.05,
                "capacity", 100
        ));
        Map<String, Object> quietCrew = post("dispatcher", "/crews", Map.of(
                "name", "Экипаж NFR низкий риск",
                "status", "READY",
                "contactChannel", "nfr-low-risk",
                "memberCount", 4,
                "assignedLogin", "crew"
        ));
        Map<String, Object> lowMission = post("dispatcher", "/missions", Map.of(
                "title", "NFR sanity low risk",
                "zoneId", 1,
                "harvesterId", id(quietHarvester),
                "crewId", id(quietCrew),
                "plannedStart", plannedStart.toString(),
                "plannedEnd", plannedStart.plusSeconds(3600).toString(),
                "route", List.of(
                        Map.of("seqNo", 1, "lat", 24.42, "lon", 54.20),
                        Map.of("seqNo", 2, "lat", 24.45, "lon", 54.24)
                )
        ));

        Map<String, Object> loudHarvester = post("dispatcher", "/harvesters", Map.of(
                "name", "HV-NFR-LOUD",
                "type", "HEAVY",
                "status", "READY",
                "noiseLevel", 0.90,
                "capacity", 100
        ));
        Map<String, Object> loudCrew = post("dispatcher", "/crews", Map.of(
                "name", "Экипаж NFR высокий риск",
                "status", "READY",
                "contactChannel", "nfr-high-risk",
                "memberCount", 4,
                "assignedLogin", "crew"
        ));
        Map<String, Object> highMission = post("dispatcher", "/missions", Map.of(
                "title", "NFR sanity high risk",
                "zoneId", 3,
                "harvesterId", id(loudHarvester),
                "crewId", id(loudCrew),
                "plannedStart", plannedStart.toString(),
                "plannedEnd", plannedStart.plusSeconds(3600).toString(),
                "route", List.of(
                        Map.of("seqNo", 1, "lat", 20.01, "lon", 51.74),
                        Map.of("seqNo", 2, "lat", 20.22, "lon", 51.95),
                        Map.of("seqNo", 3, "lat", 20.44, "lon", 52.14),
                        Map.of("seqNo", 4, "lat", 20.70, "lon", 52.40)
                )
        ));

        Map<String, Object> lowRisk = post("dispatcher", "/missions/" + id(lowMission) + "/risk-assessments", Map.of());
        Map<String, Object> highRisk = post("dispatcher", "/missions/" + id(highMission) + "/risk-assessments", Map.of());

        assertThat((Double) lowRisk.get("pAttack")).isBetween(0.0, 1.0);
        assertThat((Double) highRisk.get("pAttack")).isBetween(0.0, 1.0);
        assertThat((Integer) lowRisk.get("riskScore")).isBetween(0, 100);
        assertThat((Integer) highRisk.get("riskScore")).isBetween(0, 100);
        assertThat((Double) highRisk.get("pAttack")).isGreaterThan((Double) lowRisk.get("pAttack"));
        assertThat((Integer) highRisk.get("riskScore")).isGreaterThan((Integer) lowRisk.get("riskScore"));
    }

    @Test
    void prometheusEndpointExposesDomainMetricsWithoutJwt() {
        try {
            HttpRequest healthRequest = HttpRequest.newBuilder(URI.create(rootUrl("/actuator/health")))
                    .GET()
                    .build();
            HttpResponse<String> healthResponse = httpClient.send(healthRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(healthResponse.statusCode()).as(healthResponse.body()).isEqualTo(200);
            assertThat(healthResponse.body())
                    .contains("hsmsModules")
                    .contains("common")
                    .contains("mission")
                    .contains("risk")
                    .contains("security")
                    .contains("insurance");

            HttpRequest request = HttpRequest.newBuilder(URI.create(rootUrl("/actuator/prometheus")))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            assertThat(response.body())
                    .contains("hsms_missions_active")
                    .contains("hsms_incidents_open")
                    .contains("hsms_insurance_cases_open")
                    .contains("hsms_module_health")
                    .contains("module=\"common\"")
                    .contains("module=\"mission\"")
                    .contains("module=\"risk\"")
                    .contains("module=\"security\"")
                    .contains("module=\"insurance\"");
        } catch (IOException | InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("HTTP request failed: GET /actuator/prometheus", error);
        }
    }

    private Map<String, Object> post(String actor, String path, Object body) {
        return jsonRequest(actor, "POST", path, body);
    }

    private Map<String, Object> getJson(String actor, String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url(path)))
                    .header("Authorization", "Bearer " + token(actor))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(response.body()).isBetween(200, 299);
            return objectMapper.readValue(response.body(), new TypeReference<>() {
            });
        } catch (IOException | InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("HTTP request failed: GET " + path, error);
        }
    }

    private Map<String, Object> jsonRequest(String actor, String method, String path, Object body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url(path)))
                    .header("Content-Type", "application/json");
            if (!path.equals("/auth/login")) {
                builder.header("Authorization", "Bearer " + token(actor));
            }
            HttpRequest request = builder.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(response.body()).isBetween(200, 299);
            return objectMapper.readValue(response.body(), new TypeReference<>() {
            });
        } catch (IOException | InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("HTTP request failed: " + method + " " + path, error);
        }
    }

    private int status(String actor, String method, String path, Object body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url(path)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token(actor));
            HttpRequest request = builder.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
        } catch (IOException | InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("HTTP request failed: " + method + " " + path, error);
        }
    }

    private long id(Map<String, Object> map) {
        return ((Number) map.get("id")).longValue();
    }

    private String token(String actor) {
        return tokens.computeIfAbsent(actor, login -> {
            Map<String, Object> response = jsonRequest(login, "POST", "/auth/login", Map.of(
                    "login", login,
                    "password", "hsms123"
            ));
            return (String) response.get("token");
        });
    }

    private String url(String path) {
        return rootUrl("/api/v1" + path);
    }

    private String rootUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private <T> Timed<T> timed(CheckedSupplier<T> supplier) {
        Instant started = Instant.now();
        T value = supplier.get();
        return new Timed<>(value, Duration.between(started, Instant.now()));
    }

    private record Timed<T>(T value, Duration elapsed) {
    }

    private interface CheckedSupplier<T> {
        T get();
    }
}
