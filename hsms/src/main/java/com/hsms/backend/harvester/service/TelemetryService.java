package com.hsms.backend.harvester.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import static com.hsms.backend.common.HsmsOps.blankToDefault;
import static com.hsms.backend.common.HsmsOps.badRequest;

@Service
public class TelemetryService {

    private static final Duration MAX_FUTURE_CLOCK_SKEW = Duration.ofMinutes(2);
    private static final Duration STALE_TELEMETRY_AGE = Duration.ofMinutes(5);

    public String normalizeEquipmentStatus(String status) {
        return blankToDefault(status, "NORMAL").trim().toUpperCase(Locale.ROOT);
    }

    public void validateCoordinates(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw badRequest("Координаты телеметрии вне допустимого диапазона", "Проверьте широту и долготу пакета.");
        }
    }

    public void validateEventTime(Instant eventTime, Instant receivedAt) {
        if (eventTime.isAfter(receivedAt.plus(MAX_FUTURE_CLOCK_SKEW))) {
            throw badRequest("Timestamp телеметрии находится в будущем", "Проверьте часы полевого контура и повторите передачу пакета.");
        }
    }

    public boolean isDelayed(Instant eventTime, Instant receivedAt) {
        return eventTime.isBefore(receivedAt.minus(STALE_TELEMETRY_AGE));
    }
}
