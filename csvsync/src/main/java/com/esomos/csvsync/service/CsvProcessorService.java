package com.esomos.csvsync.service;


import java.util.Arrays;


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
    private final CreateTableService createTableService;
    private final CsvUtils csvUtils = new CsvUtils();

    public CsvProcessorService(CreateTableService createTableService) {
        this.createTableService = createTableService;
    }

   public void processCsv(String filePath) throws Exception {
    char delimiter = CsvUtils.inferDelimiter(filePath);
    try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(new BOMInputStream(new FileInputStream(filePath))))
    .withCSVParser(new CSVParserBuilder().withSeparator(delimiter).build())
    .build())  {

        String[] headers = reader.readNext();
       // System.out.println("Headers on processCsv method: " + Arrays.toString(headers) + ", Headers amount: " + (headers != null ? headers.length : 0));
                String[] cleanedHeaders = Arrays.stream(headers).map(csvUtils::cleanColumnName).toArray(String[]::new);
        if (cleanedHeaders != null) {
            createTableService.createTable(cleanedHeaders, filePath);
            String[] data;
            while ((data = reader.readNext()) != null) {
                createTableService.insertData(cleanedHeaders, data, filePath);
            }
        }
    } catch (Exception e) {
        System.err.println("Error procesando el archivo CSV: " + e.getMessage());
        throw e;
    }
}

}
