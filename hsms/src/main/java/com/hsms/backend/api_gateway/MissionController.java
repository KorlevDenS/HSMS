package com.hsms.backend.api_gateway;

import com.hsms.backend.dispatch.api.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mission")
public class MissionController {

    private final DispatchApi dispatchApi;

    public MissionController(DispatchApi dispatchApi) {
        this.dispatchApi = dispatchApi;
    }

    // mission plan and route should be also created by dispatchApi.createMission(...)
    @PostMapping("/")
    public ResponseEntity<MissionResponse> createMission(@RequestBody MissionRequest mission) {
        return ResponseEntity.ok(dispatchApi.createMission(mission));
    }

    @GetMapping("/")
    public ResponseEntity<List<MissionResponse>> getAllMissions() {
        return ResponseEntity.ok(dispatchApi.getAllMissions());
    }

    @GetMapping("/{missionId}")
    public ResponseEntity<MissionResponse> getMissionById(@PathVariable Long missionId) {
        return ResponseEntity.ok(dispatchApi.getMissionById(missionId));
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<MissionRouteResponse> getRouteById(@PathVariable Long routeId) {
        return ResponseEntity.ok(dispatchApi.getRouteById(routeId));
    }

    @GetMapping("/{planId}")
    public ResponseEntity<MissionPlanResponse> getPlanById(@PathVariable Long planId) {
        return ResponseEntity.ok(dispatchApi.getPlanById(planId));
    }

}
