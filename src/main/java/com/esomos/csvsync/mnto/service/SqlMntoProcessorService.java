package com.esomos.csvsync.mnto.service;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            // Leer el contenido del archivo con codificación Windows-1252 (ANSI)
            String content = Files.readString(Paths.get(filePath), Charset.forName("Windows-1252"));

            // Usar expresión regular para encontrar cadenas entre comillas simples
            Pattern pattern = Pattern.compile("'([^']*)'"); // Encuentra cadenas dentro de comillas simples
            Matcher matcher = pattern.matcher(content);

            // Reemplazar cualquier punto y coma dentro de las comillas simples por un guion
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                // Reemplazamos los puntos y coma dentro de las cadenas encontradas por un guion
                String updated = matcher.group(1).replace(";", "-");
                matcher.appendReplacement(sb, "'" + updated + "'");
            }
            matcher.appendTail(sb);

            // Obtener el contenido procesado sin los puntos y coma dentro de comillas
            content = sb.toString();

            // Buscar el nombre de la tabla en el CREATE TABLE
            Pattern createTablePattern = Pattern.compile("CREATE TABLE\\s+(\\w+)\\s*\\(", Pattern.CASE_INSENSITIVE);
            Matcher createTableMatcher = createTablePattern.matcher(content);

            String originalTableName = null;
            if (createTableMatcher.find()) {
                originalTableName = createTableMatcher.group(1); // Captura el nombre de la tabla
                System.out.println("Table name found: " + originalTableName);
            }

            if (originalTableName == null) {
                throw new Exception("Table name not found in the file.");
            }

            // Reemplazar el nombre de la tabla en los INSERT INTO
            content = content.replaceAll("(?i)INSERT INTO\\s+" + originalTableName, "INSERT INTO costos2025");

            // Eliminar el CREATE TABLE
            content = content.replaceAll("(?is)CREATE TABLE.*?\\);", "");

            // Reemplazar las comas decimales con puntos
            content = content.replaceAll("(\\d+),(\\d+)", "$1.$2");
            // Eliminar los milisegundos de las fechas
            content = content.replace(" 0:0:0.000", "");

            // Guardar el contenido procesado en el mismo archivo
            Files.writeString(Paths.get(filePath), content, Charset.forName("Windows-1252"));

            System.out.println("File processed successfully: " + filePath);

        } catch (java.io.IOException e) {
            System.err.println("File I/O error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error processing SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
