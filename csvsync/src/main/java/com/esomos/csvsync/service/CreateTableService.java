package com.esomos.csvsync.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.esomos.csvsync.cvsUtils.CsvUtils;

@Service
public class CreateTableService {

    private final JdbcTemplate jdbcTemplate;
    private final CsvUtils csvUtils = new CsvUtils();

    public CreateTableService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

    }

    public void createTable(String[] headers, String filePath) {
        String schemaName = obtainTableName(filePath);
        StringBuilder sqlSchema = new StringBuilder("CREATE SCHEMA IF NOT EXISTS ");
        sqlSchema.append(schemaName).append(";");

        StringBuilder sqlTable = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sqlTable.append(schemaName).append(".").append(schemaName).append(" (");

        try {

            for (String header : headers) {
                String cleanedHeader = csvUtils.cleanColumnName(header);
                sqlTable.append(cleanedHeader).append(" VARCHAR(255), ");
            }
            sqlTable.setLength(sqlTable.length() - 2);
            sqlTable.append(");");

            jdbcTemplate.execute(sqlSchema.toString());
            System.out.println("Schema created successfully: " + schemaName);
            jdbcTemplate.execute(sqlTable.toString());
            System.out.println("Table created successfully: " + schemaName + "." + schemaName);
        } catch (Exception e) {
            System.err.println("Error creating schema or table (Exception): " + e.getMessage());
        }
    }

    public void insertData(String[] headers, String[] data, String filePath) {
        String schemaName = obtainTableName(filePath);
        StringBuilder sqlInsert = new StringBuilder("INSERT INTO ");
        sqlInsert.append(schemaName).append(".").append(schemaName).append(" (");

        for (String header : headers) {
            String cleanHeader = csvUtils.cleanColumnName(header);
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

    private String obtainTableName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("\\") + 1, filePath.lastIndexOf(".")).replaceAll("[^a-zA-Z0-9]",
                "_");
    }
}
