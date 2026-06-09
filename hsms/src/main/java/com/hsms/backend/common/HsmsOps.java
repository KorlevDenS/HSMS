package com.hsms.backend.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;

public final class HsmsOps {

    private HsmsOps() {
    }

    public static HsmsException badRequest(String message, String action) {
        return new HsmsException(400, message, action);
    }

    public static HsmsException forbidden(String message, String action) {
        return new HsmsException(403, message, action);
    }

    public static HsmsException notFound(String message, String action) {
        return new HsmsException(404, message, action);
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static String blankToDefault(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    public static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static String csv(Object value) {
        if (value == null) {
            return "";
        }
        return "\"" + String.valueOf(value).replace("\"", "\"\"") + "\"";
    }

    public static void requireRoute(Collection<?> route) {
        if (route == null || route.size() < 2) {
            throw badRequest("Маршрут должен содержать минимум две точки", "Добавьте координаты маршрута харвестера.");
        }
    }

    public static void requirePlanningWindow(Instant start, Instant end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw badRequest("Плановое окно добычи заполнено неверно", "Укажите корректное время начала и окончания.");
        }
    }
}
