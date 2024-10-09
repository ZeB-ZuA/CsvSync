package com.esomos.csvsync.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;


@Service
public class CreateTableService {

    private final JdbcTemplate jdbcTemplate;

    public CreateTableService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createTable(String[] headers, String filePath) {
        StringBuilder sqlTable = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sqlTable.append(obtainTableName(filePath)).append(" (");

        for (String header : headers) {
            sqlTable.append(header).append(" VARCHAR(255), ");
        }
        sqlTable.setLength(sqlTable.length() - 2);
        sqlTable.append(");");

        try {
            jdbcTemplate.execute(sqlTable.toString());
            System.out.println("Table created successfully: " + obtainTableName(filePath));
        } catch (Exception e) {
            System.err.println("Error creating table: " + e.getMessage());
        }
    }

    public void insertData(String[] headers, String[] data, String filePath) {
        StringBuilder sqlInsert = new StringBuilder("INSERT INTO ");
        sqlInsert.append(obtainTableName(filePath)).append(" (");

        for (String header : headers) {
            sqlInsert.append(header).append(", ");
        }
        sqlInsert.setLength(sqlInsert.length() - 2); // Elimina la última coma
        sqlInsert.append(") VALUES (");

        for (String value : data) {
            sqlInsert.append("'").append(value).append("', ");
        }
        sqlInsert.setLength(sqlInsert.length() - 2); // Elimina la última coma
        sqlInsert.append(");");

        try {
            jdbcTemplate.update(sqlInsert.toString());
            System.out.println("Data inserted successfully into: " + obtainTableName(filePath));
        } catch (Exception e) {
            System.err.println("Error inserting data: " + e.getMessage());
        }
    }

    private String obtainTableName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("\\") + 1, filePath.lastIndexOf(".")).replaceAll("[^a-zA-Z0-9]",
                "_");
    }
}
