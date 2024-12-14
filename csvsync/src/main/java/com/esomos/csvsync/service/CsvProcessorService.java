package com.esomos.csvsync.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.apache.commons.io.input.BOMInputStream;

import com.esomos.csvsync.cvsUtils.CsvUtils;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class CsvProcessorService {
    private final DataBaseService dataBaseService;
    private final CsvUtils csvUtils = new CsvUtils();

    public CsvProcessorService(DataBaseService createTableService) {
        this.dataBaseService = createTableService;
    }

    public void processCsv(String filePath) throws Exception {
        char delimiter = CsvUtils.inferDelimiter(filePath);
        String[] cleanedHeaders = null;
        String dbName = dataBaseService.obtainTableName(filePath);
    
        try {
            // Crear la base de datos y cambiar la conexión
            dataBaseService.createDatabase(dbName);
            dataBaseService.changeDBconnection(dbName);
    
            try (CSVReader reader = new CSVReaderBuilder(
                    new InputStreamReader(new BOMInputStream(new FileInputStream(filePath)))).withCSVParser(
                            new CSVParserBuilder().withSeparator(delimiter).build())
                    .build()) {
    
                // Leer los encabezados y limpiarlos
                String[] headers = reader.readNext();
                cleanedHeaders = Arrays.stream(headers).map(csvUtils::cleanColumnName).toArray(String[]::new);
    
                if (cleanedHeaders != null) {
                    // Inferir los tipos de las columnas a partir de una fila de muestra
                    String[] sampleRow = CsvUtils.readSampleRow(filePath, delimiter);
                    String[] columnTypes = new String[headers.length]; // Ahora es String[] para los tipos inferidos
    
                    for (int i = 0; i < sampleRow.length; i++) {
                        columnTypes[i] = csvUtils.inferDataType(sampleRow[i]); // Aquí almacenas los tipos como String
                    }
    
                    // Crear la tabla en la base de datos
                    dataBaseService.createTable(cleanedHeaders, filePath);
    
                    // Procesar los datos en lotes
                    List<String[]> batch = new ArrayList<>();
                    int batchSize = 500; // Tamaño del lote
                    int rowsInserted = 0;
    
                    String[] data;
                    while ((data = reader.readNext()) != null) {
                        batch.add(data);
    
                        // Si el lote alcanza el tamaño definido, insertar los datos
                        if (batch.size() >= batchSize) {
                            // Insertar el lote, pasando los tipos de columnas para la conversión
                            dataBaseService.insertBatch(cleanedHeaders, batch, filePath, columnTypes);
                            rowsInserted += batch.size();
                            batch.clear(); // Limpiar el lote
                        }
                    }
    
                    // Insertar los datos restantes si quedaron filas en el lote
                    if (!batch.isEmpty()) {
                        dataBaseService.insertBatch(cleanedHeaders, batch, filePath, columnTypes);
                        rowsInserted += batch.size();
                    }
    
                    // Resumen de inserciones
                    System.out.println("Total rows inserted: " + rowsInserted);
                }
            } catch (Exception e) {
                System.err.println("Error processing CSV: " + e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            System.err.println("Error creating the database: " + e.getMessage());
            throw e;
        }
    }
    
    

}
