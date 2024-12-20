package com.esomos.csvsync.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import com.esomos.csvsync.config.DatabaseConfig;
@Service
public class DatabaseConnectionManager {

    private final JdbcTemplate jdbcTemplate;

    // Los datos de conexión son definidos de forma estática, de acuerdo con las carpetas
    private final Map<String, DatabaseConfig> databaseConfigMap = Map.of(
            "mnto", new DatabaseConfig("localhost", "5433", "mnto", "1234", "postgres"),
            "ti", new DatabaseConfig("localhost", "5434", "ti", "1234", "postgres"),
            "sgv", new DatabaseConfig("localhost", "5435", "sgv", "1234", "postgres")
    );

    public DatabaseConnectionManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Cambiar la base de datos según la carpeta
    public void changeInstanceBasedOnFolder(String folderPath) {
        for (String folderKey : databaseConfigMap.keySet()) {
            if (folderPath.contains(folderKey)) {
                DatabaseConfig config = databaseConfigMap.get(folderKey);
                changeDBConnection(config.getHost(), config.getPort(), config.getUsername(), config.getPassword(), config.getDatabase());
                return;
            }
        }
        throw new IllegalArgumentException("No se pudo determinar la base de datos para la carpeta: " + folderPath);
    }

    // Cambiar la conexión a la base de datos
    public void changeDBConnection(String host, String port, String username, String password, String database) {
        String dbUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        jdbcTemplate.setDataSource(dataSource);
        System.out.println("Conexión cambiada a la base de datos: " + database);
    }

    // Obtener la información de la base de datos actual
    public Map<String, String> getDatabaseInfo() {
        try {
            String sql = "SELECT current_user AS user, inet_server_port() AS port, current_database() AS db";
            Map<String, Object> result = jdbcTemplate.queryForMap(sql);
            Map<String, String> dbInfo = new HashMap<>();
            dbInfo.put("user", result.get("user").toString());
            dbInfo.put("port", result.get("port").toString());
            dbInfo.put("db", result.get("db").toString());
            return dbInfo;
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener la información de la base de datos actual", e);
        }
    }

    public DatabaseConfig getDatabaseConfigByPort(String port) {
        for (DatabaseConfig config : databaseConfigMap.values()) {
            if (config.getPort().equals(port)) {
                return config;
            }
        }
        throw new IllegalArgumentException("No se pudo encontrar la configuración de la base de datos para el puerto: " + port);
    }
}

