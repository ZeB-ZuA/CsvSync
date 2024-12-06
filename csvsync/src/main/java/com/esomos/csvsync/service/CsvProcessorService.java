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
    private final DataBaseService dataBaseService;
    private final CsvUtils csvUtils = new CsvUtils();
   


    
    public CsvProcessorService(DataBaseService createTableService) {
        this.dataBaseService = createTableService;
    }

   public void processCsv(String filePath) throws Exception {
        char delimiter = CsvUtils.inferDelimiter(filePath);
        int rowCount = countRows(filePath, delimiter);
        int rowsInserted = 0;
        String[] cleanedHeaders = null;
        String dbName = dataBaseService.obtainTableName(filePath);

        try {
          
            dataBaseService.createDatabase(dbName);
            dataBaseService.changeDBconnection(dbName);
            try (CSVReader reader = new CSVReaderBuilder(
                    new InputStreamReader(new BOMInputStream(new FileInputStream(filePath)))).withCSVParser(
                            new CSVParserBuilder().withSeparator(delimiter).build())
                    .build()) {

                String[] headers = reader.readNext();
                cleanedHeaders = Arrays.stream(headers).map(csvUtils::cleanColumnName).toArray(String[]::new);

                if (cleanedHeaders != null) {
                    dataBaseService.createTable(cleanedHeaders, filePath);

                    String[] data;
                    while ((data = reader.readNext()) != null) {
                        dataBaseService.insertData(cleanedHeaders, data, filePath, rowCount);
                        rowsInserted++;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing CSV: " + e.getMessage());
                throw e;
            }

        } catch (Exception e) {
            System.err.println("Error creating the database: " + e.getMessage());
            throw e;
        }
        System.out.println("Summary:");
        System.out.println("Total rows from CSV: " + rowCount);
        System.out.println("Rows inserted: " + rowsInserted);
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
