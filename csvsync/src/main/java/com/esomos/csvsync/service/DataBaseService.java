package com.esomos.csvsync.service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import com.esomos.csvsync.cvsUtils.CsvUtils;

@Service

public class DataBaseService {

    private final JdbcTemplate jdbcTemplate;
    private final CsvUtils csvUtils = new CsvUtils();

     private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;
    private final String dbPort;

    @Autowired
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
            System.out.println("Base de datos " + dbName + " creada exitosamente.");
        } catch (Exception e) {
            if (e.getMessage().contains("already exists")) {
                System.out.println("La base de datos " + dbName + " ya existe.");
            } else {
                System.err.println("Error al crear la base de datos: " + e.getMessage());
            }
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
            System.out.println("Conexión cambiada a la base de datos: " + dbName);
        } catch (Exception e) {
            System.err.println("Error al cambiar la conexión a la base de datos: " + e.getMessage());
        }
    }

    public void createTable(String[] headers, String filePath) {
        System.out.println(
                "Headers on createTable method: " + Arrays.toString(headers) + ", Headers amount: " + headers.length);
        String schemaName = obtainTableName(filePath);
        StringBuilder sqlSchema = new StringBuilder("CREATE SCHEMA IF NOT EXISTS ");
        sqlSchema.append(schemaName).append(";");

        StringBuilder sqlTable = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sqlTable.append(schemaName).append(".").append(schemaName).append(" (");

        try {
            char delimiter = CsvUtils.inferDelimiter(filePath);
            System.out.println("Delimiter: " + delimiter);
            String[] sampleRow = CsvUtils.readSampleRow(filePath, delimiter);

            for (int i = 0; i < headers.length; i++) {
                // String header = csvUtils.cleanColumnName(headers[i]);
                String header = headers[i];
                String inferredType = csvUtils.inferDataType(sampleRow[i]);
                System.out.println("Columna: " + header + ", Valor de muestra: " + sampleRow[i]
                        + ", Tipo de dato inferido: " + inferredType);

                if ("DOUBLE".equalsIgnoreCase(inferredType)) {
                    inferredType = "DOUBLE PRECISION";
                } else if ("DATETIME".equalsIgnoreCase(inferredType) || "DATE".equalsIgnoreCase(inferredType)) {
                    inferredType = "TIMESTAMP";
                }

                sqlTable.append(header).append(" ").append(inferredType).append(", ");
            }

            sqlTable.setLength(sqlTable.length() - 2);
            sqlTable.append(");");

            jdbcTemplate.execute(sqlSchema.toString());
            System.out.println("Schema created successfully: " + schemaName);
            jdbcTemplate.execute(sqlTable.toString());
            System.out.println("Table created successfully: " + schemaName + "." + schemaName);
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error creating schema or table(Exception): " + e.getMessage());
        }
    }

    public void insertData(String[] headers, String[] data, String filePath, int rowCount) throws IOException {
        String schemaName = obtainTableName(filePath);

        // System.out.println("Headers on insertData method: " +
        // Arrays.toString(headers) + ", Headers amount: " + headers.length);
        StringBuilder sqlInsert = new StringBuilder("INSERT INTO ");
        sqlInsert.append(schemaName).append(".").append(schemaName).append(" (");

        for (String header : headers) {
            // String cleanHeader = csvUtils.cleanColumnName(header);
            String cleanHeader = header;
            sqlInsert.append(cleanHeader).append(", ");
        }
        sqlInsert.setLength(sqlInsert.length() - 2);
        sqlInsert.append(") VALUES (");

        for (String value : data) {
            sqlInsert.append("'").append(value.replace("'", "''")).append("', ");
        }
        sqlInsert.setLength(sqlInsert.length() - 2);
        sqlInsert.append(");");

        try {
            jdbcTemplate.update(sqlInsert.toString());
            System.out.println("Data inserted successfully into: " + schemaName + "." + schemaName);

        } catch (Exception e) {
            System.err.println("Error inserting data: " + e.getMessage());
        }

    }

    public String obtainTableName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("\\") + 1, filePath.lastIndexOf(".")).replaceAll("[^a-zA-Z0-9]",
                "_");
    }

}
