package com.esomos.csvsync.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.apache.commons.io.input.BOMInputStream;

import com.esomos.csvsync.cvsUtils.CsvUtils;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.FileInputStream;
import java.io.InputStreamReader;

@Service
public class CsvProcessorService {
    @Lazy
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
            dataBaseService.createDatabase(dbName);
            dataBaseService.changeDBconnection(dbName);

            try (CSVReader reader = new CSVReaderBuilder(
                    new InputStreamReader(new BOMInputStream(new FileInputStream(filePath)))).withCSVParser(
                            new CSVParserBuilder().withSeparator(delimiter).build())
                    .build()) {

                String[] headers = reader.readNext();
                cleanedHeaders = Arrays.stream(headers).map(csvUtils::cleanColumnName).toArray(String[]::new);

                if (cleanedHeaders != null) {
                    String[] sampleRow = CsvUtils.readSampleRow(filePath, delimiter);
                    String[] columnTypes = new String[headers.length];

                    for (int i = 0; i < sampleRow.length; i++) {
                        columnTypes[i] = csvUtils.inferDataType(sampleRow[i]);
                    }

                    dataBaseService.createTable(cleanedHeaders, filePath);

                    List<String[]> batch = new ArrayList<>();
                    int batchSize = 500;
                    int rowsInserted = 0;

                    String[] data;
                    while ((data = reader.readNext()) != null) {
                        batch.add(data);

                        if (batch.size() >= batchSize) {
                            dataBaseService.insertBatch(cleanedHeaders, batch, filePath, columnTypes);
                            rowsInserted += batch.size();
                            batch.clear();
                        }
                    }

                    if (!batch.isEmpty()) {
                        dataBaseService.insertBatch(cleanedHeaders, batch, filePath, columnTypes);
                        rowsInserted += batch.size();
                    }

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
