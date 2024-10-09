package com.esomos.csvsync.service;

import java.io.FileReader;

import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;

@Service
public class CsvProcessorService {
    private final CreateTableService createTableService;

    public CsvProcessorService(CreateTableService createTableService) {
        this.createTableService = createTableService;
    }

    public void processCsv(String filePath) throws Exception {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            if (headers != null) {
                createTableService.createTable(headers, filePath); 
                String[] data;
                while ((data = reader.readNext()) != null) {
                
                    createTableService.insertData(headers, data, filePath);
                }
            }
        } catch (Exception e) {
            
            System.err.println("Error procesando el archivo CSV: " + e.getMessage());
            throw e;
        }
    }
}
