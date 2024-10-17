package com.esomos.csvsync.service;

import java.io.IOException;
import java.util.Arrays;

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
        System.out.println("Headers on createTable method: " + Arrays.toString(headers) + ", Headers amount: " + headers.length);
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
                //String header = csvUtils.cleanColumnName(headers[i]);
                String header = headers[i];
                String inferredType = csvUtils.inferDataType(sampleRow[i]);
                System.out.println("Columna: " + header + ", Valor de muestra: " + sampleRow[i] + ", Tipo de dato inferido: " + inferredType);
                
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
    
    
    
    

    public void insertData(String[] headers, String[] data, String filePath) {
        String schemaName = obtainTableName(filePath);
        //System.out.println("Headers on insertData method: " + Arrays.toString(headers) + ", Headers amount: " + headers.length);
        StringBuilder sqlInsert = new StringBuilder("INSERT INTO ");
        sqlInsert.append(schemaName).append(".").append(schemaName).append(" (");

        for (String header : headers) {
            //String cleanHeader = csvUtils.cleanColumnName(header);
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

    private String obtainTableName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("\\") + 1, filePath.lastIndexOf(".")).replaceAll("[^a-zA-Z0-9]",
                "_");
    }
}
