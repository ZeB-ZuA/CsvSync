package com.esomos.csvsync.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.esomos.csvsync.config.DatabaseConfig;
import com.esomos.csvsync.cvsUtils.CsvUtils;

@Service
public class DataBaseService {

    private static final Logger logger = LoggerFactory.getLogger(DataBaseService.class);

    private final JdbcTemplate jdbcTemplate;
    private final CsvUtils csvUtils;
    private final DatabaseConnectionManager connectionManager;

    public DataBaseService(
            JdbcTemplate jdbcTemplate,
            CsvUtils csvUtils,
            DatabaseConnectionManager connectionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.csvUtils = csvUtils;
        this.connectionManager = connectionManager;

    }

    public void createDatabase(String dbName) {
        try {
            String createDbSQL = "CREATE DATABASE " + dbName;
            jdbcTemplate.execute(createDbSQL);
            System.out.println("Database '{}' created successfully." + dbName);
        } catch (Exception e) {
            logger.error("Error creating database '{}': {}", dbName, e.getMessage(), e);
            throw e;
        }
    }

    public void changeDBconnection(String dbName) {
        try {
            // Obtener la información actual de la base de datos
            Map<String, String> dbInfo = getCurrentDatabaseInfo();
            String currentPort = dbInfo.get("port");

            // Obtener la configuración de la base de datos basada en el puerto
            DatabaseConfig config = connectionManager.getDatabaseConfigByPort(currentPort);

            // Cambiar la conexión a la nueva base de datos
            connectionManager.changeDBConnection(config.getHost(), config.getPort(), config.getUsername(),
                    config.getPassword(), dbName);

            System.out.println("Conexión cambiada a la base de datos: " + dbName);
        } catch (Exception e) {
            logger.error("Error cambiando la conexión a la base de datos '{}': {}", dbName, e.getMessage(), e);
            throw e;
        }
    }

    public Map<String, String> getCurrentDatabaseInfo() {
        try {
            String sql = "SELECT current_user AS user, "
                    + "inet_server_port() AS port, "
                    + "current_database() AS db";

            Map<String, Object> result = jdbcTemplate.queryForMap(sql);
            Map<String, String> dbInfo = new HashMap<>();
            dbInfo.put("user", result.get("user").toString());
            dbInfo.put("port", result.get("port").toString());
            dbInfo.put("db", result.get("db").toString());

            return dbInfo;
        } catch (Exception e) {
            logger.error("Error fetching current database information: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void createTable(String[] headers, String filePath) {
        String tableName = obtainTableName(filePath);
        StringBuilder sqlTable = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sqlTable.append(tableName).append(" (");

        try {
            char delimiter = CsvUtils.inferDelimiter(filePath);
            String[] sampleRow = CsvUtils.readSampleRow(filePath, delimiter);

            for (int i = 0; i < headers.length; i++) {
                String header = headers[i];
                String inferredType = csvUtils.inferDataType(sampleRow[i]);

                sqlTable.append(header).append(" ").append(inferredType).append(", ");
            }

            sqlTable.setLength(sqlTable.length() - 2);
            sqlTable.append(");");

            jdbcTemplate.execute(sqlTable.toString());
            System.out.println("Table created successfully: " + tableName);
        } catch (IOException e) {
            logger.error("Error reading CSV file at path '{}': {}", filePath, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error creating table for '{}': {}", tableName, e.getMessage(), e);
        }
    }

    public void insertBatch(String[] headers, List<String[]> dataBatch, String filePath, String[] columnTypes) {
        String tableName = obtainTableName(filePath);
        StringBuilder sqlInsert = new StringBuilder("INSERT INTO ");
        sqlInsert.append(tableName).append(" (");
        for (String header : headers) {
            sqlInsert.append(header).append(", ");
        }
        sqlInsert.setLength(sqlInsert.length() - 2);
        sqlInsert.append(") VALUES ");
        String placeholders = "(" + String.join(", ", Collections.nCopies(headers.length, "?")) + ")";
        sqlInsert.append(placeholders);
        sqlInsert.append(";");

        List<Object[]> batchParams = new ArrayList<>();
        for (String[] row : dataBatch) {
            Object[] batchRow = new Object[row.length];

            for (int i = 0; i < row.length; i++) {
                batchRow[i] = convertToProperType(row[i], columnTypes[i]);
            }

            batchParams.add(batchRow);
        }

        try {
            int[] rowsInserted = jdbcTemplate.batchUpdate(sqlInsert.toString(), batchParams);
            System.out.println("Batch insert completed. Rows inserted: " + Arrays.stream(rowsInserted).sum());
        } catch (Exception e) {
            logger.error("Error performing batch insert into '{}': {}", tableName, e.getMessage(), e);
            throw e;
        }
    }

    public String obtainTableName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("\\") + 1, filePath.lastIndexOf(".")).replaceAll("[^a-zA-Z0-9]",
                "_");
    }

    private Object convertToProperType(String value, String columnType) {
        switch (columnType.toUpperCase()) {
            case "INTEGER":
            case "INT":
                return Integer.parseInt(value);
            case "BOOLEAN":
                return Boolean.parseBoolean(value);
            case "DATE":
                return java.sql.Date.valueOf(value);
            case "TIME":
                return java.sql.Time.valueOf(value);
            case "TIMESTAMP":
                return java.sql.Timestamp.valueOf(value);
            case "DOUBLE PRECISION":
            case "DECIMAL":
                return Double.parseDouble(value);
            case "TEXT":
            case "VARCHAR":
                return value;
            default:
                throw new IllegalArgumentException("Unsupported column type: " + columnType);
        }
    }

}
