package com.esomos.csvsync.cvsUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;


import com.opencsv.*;

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

    
    public String cleanColumnName(String columnName) {
        return columnName.trim().replaceAll(" ", "_").replaceAll("[^a-zA-Z0-9_]", "");
    }
    

    public static String[] readSampleRow(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String firstLine = br.readLine();
            if (firstLine != null && firstLine.startsWith("\uFEFF")) {
                firstLine = firstLine.substring(1);
            }
    
            return firstLine.split(";");
        }
    }
}

