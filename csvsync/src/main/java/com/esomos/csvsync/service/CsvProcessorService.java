package com.esomos.csvsync.service;

import java.io.FileReader;
import java.util.Arrays;

import org.springframework.stereotype.Service;

import com.esomos.csvsync.cvsUtils.CsvUtils;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

@Service
public class CsvProcessorService {
    private final CreateTableService createTableService;

    public CsvProcessorService(CreateTableService createTableService) {
        this.createTableService = createTableService;
    }

   public void processCsv(String filePath) throws Exception {
    char delimiter = CsvUtils.inferDelimiter(filePath);
    try (CSVReader reader = new CSVReaderBuilder(new FileReader(filePath))
            .withCSVParser(new CSVParserBuilder().withSeparator(delimiter).build())
            .build()) {

        String[] headers = reader.readNext();
       // System.out.println("Headers on processCsv method: " + Arrays.toString(headers) + ", Headers amount: " + (headers != null ? headers.length : 0));

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
