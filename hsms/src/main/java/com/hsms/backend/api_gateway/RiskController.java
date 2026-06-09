package com.hsms.backend.api_gateway;

import com.hsms.backend.dispatch.api.MissionResponse;
import com.hsms.backend.risk.api.RiskApi;
import com.hsms.backend.risk.api.RiskScoreResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/risk")
public class RiskController {

    private final RiskApi riskApi;

    public RiskController(RiskApi riskApi) {
        this.riskApi = riskApi;
    }

    @PostMapping("/")
    public ResponseEntity<RiskScoreResponse> calcRiskScore(@RequestBody MissionResponse mission) {
        return ResponseEntity.ok(riskApi.calcRiskScore(mission));
    }

    @GetMapping("/{riskScoreId}")
    public ResponseEntity<RiskScoreResponse> getRiskScoreById(@PathVariable Long riskScoreId) {
        return ResponseEntity.ok(riskApi.getRiskScoreById(riskScoreId));
    }

}
