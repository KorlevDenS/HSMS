package com.hsms.backend.harvester.service;

import org.springframework.stereotype.Service;

import static com.hsms.backend.common.HsmsOps.blankToDefault;
import static com.hsms.backend.common.HsmsOps.badRequest;

@Service
public class TelemetryService {

    public String normalizeEquipmentStatus(String status) {
        return blankToDefault(status, "NORMAL").trim().toUpperCase();
    }

    public void validateCoordinates(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw badRequest("Координаты телеметрии вне допустимого диапазона", "Проверьте широту и долготу пакета.");
        }
    }
}
