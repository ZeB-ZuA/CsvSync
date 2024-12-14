package com.esomos.csvsync.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import com.esomos.csvsync.cvsUtils.CsvUtils;

@Service
public class DataBaseService {

    private static final Logger logger = LoggerFactory.getLogger(DataBaseService.class);

    private final JdbcTemplate jdbcTemplate;
    private final CsvUtils csvUtils = new CsvUtils();

    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;
    private final String dbPort;

    public DataBaseService(JdbcTemplate jdbcTemplate,
            @Value("${spring.datasource.url}") String dbUrl,
            @Value("${spring.datasource.username}") String dbUsername,
            @Value("${spring.datasource.password}") String dbPassword,
            @Value("${spring.datasource.port}") String dbPort) {
        this.jdbcTemplate = jdbcTemplate;
        this.dbUrl = dbUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.dbPort = dbPort;
    }

    public void createDatabase(String dbName) {
        try {
            String createDbSQL = "CREATE DATABASE " + dbName;
            jdbcTemplate.execute(createDbSQL);
            System.out.println("Database '" + dbName + "' created successfully.");
        } catch (Exception e) {
            logger.error("Error creating database '{}': {}", dbName, e.getMessage(), e);
            throw e;
        }
    }

    public void changeDBconnection(String dbName) {
        try {
            String newDbUrl = "jdbc:postgresql://localhost:" + dbPort + "/" + dbName;
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl(newDbUrl);
            dataSource.setUsername(dbUsername);
            dataSource.setPassword(dbPassword);
            jdbcTemplate.setDataSource(dataSource);
            System.out.println("Database connection changed to '" + dbName + "'.");
        } catch (Exception e) {
            logger.error("Error changing database connection to '{}': {}", dbName, e.getMessage(), e);
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
    
        // Agregar los nombres de las columnas al SQL
        for (String header : headers) {
            sqlInsert.append(header).append(", ");
        }
        sqlInsert.setLength(sqlInsert.length() - 2); // Eliminar la última coma
        sqlInsert.append(") VALUES ");
    
        // Construir los placeholders para las filas
        String placeholders = "(" + String.join(", ", Collections.nCopies(headers.length, "?")) + ")";
        sqlInsert.append(placeholders); // Añadir placeholders
        sqlInsert.append(";");
    
        // Crear la lista de parámetros para cada fila en el lote
        List<Object[]> batchParams = new ArrayList<>();
        for (String[] row : dataBatch) {
            Object[] batchRow = new Object[row.length];
    
            for (int i = 0; i < row.length; i++) {
                // Convertir el valor según el tipo inferido
                batchRow[i] = convertToProperType(row[i], columnTypes[i]);
            }
    
            batchParams.add(batchRow);
        }
    
        try {
            // Ejecutar el batch con JdbcTemplate
            int[] rowsInserted = jdbcTemplate.batchUpdate(sqlInsert.toString(), batchParams);
            System.out.println("Batch insert completed. Rows inserted: " + Arrays.stream(rowsInserted).sum());
        } catch (Exception e) {
            logger.error("Error performing batch insert into '{}': {}", tableName, e.getMessage(), e);
            throw e;
        }
    }
    

    
    public String obtainTableName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("\\") + 1, filePath.lastIndexOf(".")).replaceAll("[^a-zA-Z0-9]", "_");
    }

    private Object convertToProperType(String value, String columnType) {
        switch (columnType.toUpperCase()) {
            case "INTEGER":
            case "INT":
                return Integer.parseInt(value); // Convertir a Integer
            case "BOOLEAN":
                return Boolean.parseBoolean(value); // Convertir a Boolean
            case "DATE":
                return java.sql.Date.valueOf(value); // Convertir a Date (en formato YYYY-MM-DD)
            case "TIME":
                return java.sql.Time.valueOf(value); // Convertir a Time (en formato HH:MM:SS)
            case "TIMESTAMP":
                return java.sql.Timestamp.valueOf(value); // Convertir a Timestamp
            case "DOUBLE PRECISION":
            case "DECIMAL":
                return Double.parseDouble(value); // Convertir a Double
            case "TEXT":
            case "VARCHAR":
                return value; // Dejar como String
            default:
                throw new IllegalArgumentException("Unsupported column type: " + columnType);
        }
    }
    
}
