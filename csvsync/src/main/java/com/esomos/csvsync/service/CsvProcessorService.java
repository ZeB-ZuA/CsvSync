package com.esomos.csvsync.service;

import java.util.Arrays;

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
    private final CreateTableService createTableService;
    private final CsvUtils csvUtils = new CsvUtils();
    public CsvProcessorService(CreateTableService createTableService) {
        this.createTableService = createTableService;
    }
    public void processCsv(String filePath) throws Exception {
        char delimiter = CsvUtils.inferDelimiter(filePath);
        int rowCount = countRows(filePath, delimiter);
        int rowsInserted = 0;
        String[] cleanedHeaders = null;
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new BOMInputStream(new FileInputStream(filePath))))
                .withCSVParser(new CSVParserBuilder().withSeparator(delimiter).build())
                .build()) {
            String[] headers = reader.readNext();
            cleanedHeaders = Arrays.stream(headers).map(csvUtils::cleanColumnName).toArray(String[]::new);
            if (cleanedHeaders != null) {
                createTableService.createTable(cleanedHeaders, filePath);
                String[] data;
                while ((data = reader.readNext()) != null) {
                    createTableService.insertData(cleanedHeaders, data, filePath, rowCount);

                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando el archivo CSV: " + e.getMessage());
            throw e;
        }
        System.out.println("Sumary:");
        System.out.println("total rows from CSV: " + rowCount);
        System.out.println("rows inserted: " + rowsInserted);
        System.out.println("Delimiter: " + delimiter);
        System.out.println("Sample row: " + Arrays.toString(CsvUtils.readSampleRow(filePath, delimiter)));
        System.out.println("Headers: " + Arrays.toString(cleanedHeaders));
    }

    private int countRows(String filePath, char delimiter) throws IOException, CsvValidationException {
        int rowCount = 0;
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new BOMInputStream(new FileInputStream(filePath))))
                .withCSVParser(new CSVParserBuilder().withSeparator(delimiter).build())
                .build()) {

            while (reader.readNext() != null) {
                rowCount++;
            }
        }
        return rowCount - 1;
    }

}
