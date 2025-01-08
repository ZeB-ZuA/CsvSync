package com.esomos.csvsync.mnto.service;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.stereotype.Service;

import com.esomos.csvsync.service.DataBaseService;

@Service
public class SqlMntoProcessorService {

    private final DataBaseService dataBaseService;

    public SqlMntoProcessorService(DataBaseService dataBaseService) {
        this.dataBaseService = dataBaseService;
    }

    public void procesMntoSql(String filePath) throws Exception {
        try {
            String content = Files.readString(Paths.get(filePath), Charset.forName("Windows-1252")); 
            
            Pattern createTablePattern = Pattern.compile("CREATE TABLE\\s+(\\w+)\\s*\\(", Pattern.CASE_INSENSITIVE);
            Matcher createTableMatcher = createTablePattern.matcher(content);

            String originalTableName = null;
            if (createTableMatcher.find()) {
                originalTableName = createTableMatcher.group(1);
                System.out.println("Table name found: " + originalTableName);
            }

            if (originalTableName == null) {
                throw new Exception("Table name not found in the file.");
            }

            content = content.replaceAll("(?i) INSERT INTO " + originalTableName, "INSERT INTO costos2025");
            content = content.replaceAll("(?is)CREATE TABLE.*?\\);", "");
            content = content.replaceAll("(\\d+),(\\d+)", "$1.$2");

         
            Files.writeString(Paths.get(filePath), content);
            System.out.println("File processed successfully: " + filePath);

        } catch (java.io.IOException e) {
            System.err.println("File I/O error: " + e.getMessage());
            e.printStackTrace();
        } catch (PatternSyntaxException e) {
            System.err.println("Pattern syntax error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error processing SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
