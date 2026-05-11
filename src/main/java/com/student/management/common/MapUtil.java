package com.student.management.common;

import java.util.Map;

public final class MapUtil {
    private MapUtil() {
    }

    public static long longValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    public static String stringValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static boolean booleanValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = String.valueOf(value);
        return "1".equals(text) || Boolean.parseBoolean(text);
    }
}
