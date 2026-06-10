package com.hsms.backend.common;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class DomainCollections {

    private DomainCollections() {
        throw new UnsupportedOperationException("Utility class");
    }

    static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    static <T> List<T> immutableListOrNull(List<T> values) {
        return values == null ? null : List.copyOf(values);
    }

    static <K, V> Map<K, V> immutableMap(Map<K, V> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }

    static <T> Set<T> immutableSet(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }
}
