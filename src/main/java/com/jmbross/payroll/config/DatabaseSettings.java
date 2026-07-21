package com.jmbross.payroll.config;

import java.util.Map;

public record DatabaseSettings(String host, int port, String database, String user, String password) {
    public static DatabaseSettings fromEnvironment(Map<String, String> environment) {
        return new DatabaseSettings(
                required(environment, "DB_HOST", "127.0.0.1"),
                parsePort(required(environment, "DB_PORT", "3306")),
                required(environment, "DB_NAME", "payroll_demo"),
                required(environment, "DB_USER", "payroll_app"),
                required(environment, "DB_PASSWORD", null));
    }

    public String jdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false";
    }

    private static String required(Map<String, String> environment, String key, String fallback) {
        String value = environment.getOrDefault(key, fallback);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " is required");
        }
        return value;
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("DB_PORT must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("DB_PORT must be numeric", exception);
        }
    }
}
