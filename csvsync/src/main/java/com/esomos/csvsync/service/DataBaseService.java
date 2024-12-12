package com.esomos.csvsync.service;

import java.io.IOException;
import java.util.Arrays;

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
        // Obtener el nombre de la tabla desde el archivo (sin esquema)
        String tableName = obtainTableName(filePath);
        
        // Construir la sentencia SQL para la creación de la tabla
        StringBuilder sqlTable = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sqlTable.append(tableName).append(" (");
    
        try {
            // Inferir el delimitador del archivo CSV
            char delimiter = CsvUtils.inferDelimiter(filePath);
            // Leer una fila de muestra del archivo CSV para inferir los tipos de datos
            String[] sampleRow = CsvUtils.readSampleRow(filePath, delimiter);
    
            // Iterar sobre los encabezados y los valores de la fila para crear las columnas de la tabla
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i];
                String inferredType = csvUtils.inferDataType(sampleRow[i]);
    
                // Agregar cada columna con su tipo de dato a la sentencia SQL
                sqlTable.append(header).append(" ").append(inferredType).append(", ");
            }
    
            // Eliminar la última coma de la sentencia SQL
            sqlTable.setLength(sqlTable.length() - 2);
            sqlTable.append(");");
    
            // Ejecutar la sentencia SQL para crear la tabla
            jdbcTemplate.execute(sqlTable.toString());
            System.out.println("Table created successfully: " + tableName);
        } catch (IOException e) {
            logger.error("Error reading CSV file at path '{}': {}", filePath, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error creating table for '{}': {}", tableName, e.getMessage(), e);
        }
    }
    
    

    public void insertData(String[] headers, String[] data, String filePath, int rowCount) throws IOException {
        // Obtener el nombre de la tabla desde el archivo (sin esquema)
        String tableName = obtainTableName(filePath);
    
        StringBuilder sqlInsert = new StringBuilder("INSERT INTO ");
        sqlInsert.append(tableName).append(" (");
    
        // Agregar los nombres de las columnas a la sentencia SQL
        for (String header : headers) {
            String cleanHeader = header;
            sqlInsert.append(cleanHeader).append(", ");
        }
        sqlInsert.setLength(sqlInsert.length() - 2); // Eliminar la última coma
        sqlInsert.append(") VALUES (");
    
        // Agregar los valores a la sentencia SQL
        for (String value : data) {
            sqlInsert.append("'").append(value.replace("'", "''")).append("', ");
        }
        sqlInsert.setLength(sqlInsert.length() - 2); // Eliminar la última coma
        sqlInsert.append(");");
    
        try {
            // Ejecutar la sentencia SQL para insertar los datos
            jdbcTemplate.update(sqlInsert.toString());
            System.out.println("Data inserted successfully into: " + tableName);
        } catch (Exception e) {
            logger.error("Error inserting data into '{}': {}", tableName, e.getMessage(), e);
        }
    }
    

    public String obtainTableName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("\\") + 1, filePath.lastIndexOf(".")).replaceAll("[^a-zA-Z0-9]", "_");
    }
}
