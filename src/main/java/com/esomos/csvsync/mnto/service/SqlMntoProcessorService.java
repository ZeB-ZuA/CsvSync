package com.esomos.csvsync.mnto.service;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;



@Service
public class SqlMntoProcessorService {

   

    public void procesMntoSql(String filePath) throws Exception {
        Path file = Paths.get(filePath);

        try {
            waitForFileRelease(file);

    
            String content = Files.readString(file, Charset.forName("Windows-1252"));

            Pattern pattern = Pattern.compile("'([^']*)'"); 
            Matcher matcher = pattern.matcher(content);

            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String updated = matcher.group(1).replace(";", "-");
                matcher.appendReplacement(sb, "'" + updated + "'");
            }
            matcher.appendTail(sb);
            content = sb.toString();

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

            content = content.replaceAll("(?i)INSERT INTO\\s+" + originalTableName, "INSERT INTO costos2025");

            content = content.replaceAll("(?is)CREATE TABLE.*?\\);", "");

            content = content.replaceAll("(\\d+),(\\d+)", "$1.$2");

            content = content.replace(" 0:0:0.000", "");


            Files.writeString(file, content, Charset.forName("Windows-1252"));

            System.out.println("Archivo procesado exitosamente: " + filePath);

        } catch (java.io.IOException e) {
            System.err.println("Error de lectura/escritura: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error al procesar el archivo SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método para esperar hasta que un archivo deje de estar bloqueado.
     * 
     * @param filePath Ruta del archivo que se debe desbloquear.
     * @throws InterruptedException Si el hilo es interrumpido.
     */
    private void waitForFileRelease(Path filePath) throws InterruptedException {
        int retryCount = 0;
        int maxRetries = 50;
        long waitInterval = 1000; 

        while (retryCount < maxRetries) {
            try {
                try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
                    System.out.println("El archivo está desbloqueado: " + filePath);
                    return; 
                }
            } catch (IOException e) {
                System.out.println("El archivo está bloqueado, reintentando...");
                retryCount++;
                Thread.sleep(waitInterval);
            }
        }

        throw new RuntimeException("El archivo sigue bloqueado después de múltiples intentos: " + filePath);
    }

}
