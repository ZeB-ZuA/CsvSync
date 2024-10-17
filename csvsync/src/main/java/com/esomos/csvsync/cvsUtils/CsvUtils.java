package com.esomos.csvsync.cvsUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.apache.commons.csv.CSVParser;

import java.io.Reader;

public class CsvUtils {

    public String inferDataType(String value) {
        if (isBoolean(value)) {
            return "BOOLEAN";
        } else if (isInteger(value)) {
            return "INT";
        } else if (isDouble(value)) {
            return "DOUBLE PRECISION";
        } else if (isTimestamp(value)) {
            return "TIMESTAMP";
        } else if (isDate(value)) {
            return "DATE";
        } else if (isTime(value)) {
            return "TIME";
        } else {
            return "TEXT"; 
        }
    }

    public String cleanColumnName(String columnName) {
        System.out.println("berfore cleanig: " + columnName);
        String columnCleaned = columnName.trim().replaceAll(" ", "_").replaceAll("[^a-zA-Z0-9_]", "");
        System.out.println("After cleaning: " + columnCleaned);
        return columnCleaned;
       
    }

    public static String[] readSampleRow(String filePath, char delimiter) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT
                .withDelimiter(delimiter)
                .withFirstRecordAsHeader()
                .withQuote('"');
    
        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, format)) {
          //  System.out.println("Headers on readSampleRow method : " + csvParser.getHeaderNames() + ", Headers amount : " + csvParser.getHeaderNames().size());

            CSVRecord firstRecord = csvParser.getRecords().get(0);
            String[] sampleRow = new String[firstRecord.size()];
            for (int i = 0; i < firstRecord.size(); i++) {
                sampleRow[i] = firstRecord.get(i);
            }
    
            return sampleRow;
        }
    }
    
    
    

public static char inferDelimiter(String filePath) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
        String headerLine = br.readLine();
        if (headerLine != null) {
            if (headerLine.contains(";")) {
                return ';';
            } else if (headerLine.contains(",")) {
                return ',';
            }
        }
    }
    return ';'; 
}


    private static boolean isBoolean(String value) {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDate(String value) {
        return isValidFormat("yyyy-MM-dd", value) || isValidFormat("MM/dd/yyyy", value);
    }

    private static boolean isTime(String value) {
        return isValidFormat("HH:mm:ss", value) || isValidFormat("hh:mm:ss a", value);
    }

    private static boolean isTimestamp(String value) {
        return isValidFormat("yyyy-MM-dd HH:mm:ss.SSS", value) || isValidFormat("yyyy-MM-dd HH:mm:ss", value);
    }

    private static boolean isValidFormat(String format, String value) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setLenient(false);
        try {
            sdf.parse(value);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

}
