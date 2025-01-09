package com.esomos.csvsync.SqlUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

public class SqlUtils {
    public static void executeSqlFileInBatches(String filePath, JdbcTemplate jdbcTemplate) throws IOException {
        try {
            // Leer el archivo SQL con la codificación Windows-1252
            String content = Files.readString(Paths.get(filePath), Charset.forName("Windows-1252"));

            // Dividir el contenido en sentencias SQL
            String[] sqlStatements = content.split(";");

            // Lista para almacenar las partes de los INSERT que se procesarán en lotes
            List<String> batchedInserts = new ArrayList<>();
            StringBuilder currentBatch = new StringBuilder();
            int batchCount = 0;

            for (String sql : sqlStatements) {
                sql = sql.trim();
                if (sql.startsWith("INSERT INTO") && !sql.isEmpty()) {
                    // Extraer los valores de la sentencia INSERT INTO
                    int valuesStartIndex = sql.indexOf("VALUES") + 6; // Indice del inicio de VALUES
                    String valuesPart = sql.substring(valuesStartIndex).trim(); // Obtener solo los valores

                    // Asegurarse de que los valores estén entre paréntesis
                    if (!valuesPart.startsWith("(")) {
                        valuesPart = "(" + valuesPart;
                    }
                    if (!valuesPart.endsWith(")")) {
                        valuesPart = valuesPart + ")";
                    }

                    // Si el lote actual tiene menos de 500, agregamos el valor a este lote
                    if (batchCount < 500) {
                        if (currentBatch.length() > 0) {
                            currentBatch.append(", "); // Agregar coma entre los registros
                        }
                        currentBatch.append(valuesPart);
                        batchCount++;
                    } else {
                        // Cuando el lote tiene 500 registros, guardamos y comenzamos un nuevo lote
                        batchedInserts.add("INSERT INTO costos2025 VALUES " + currentBatch.toString() + ";");
                        currentBatch.setLength(0); // Limpiar el StringBuilder para el siguiente lote
                        currentBatch.append(valuesPart); // Comenzar con el nuevo valor
                        batchCount = 1; // Restablecer el contador
                    }
                }
            }

            // Añadir el último lote si tiene registros
            if (currentBatch.length() > 0) {
                batchedInserts.add("INSERT INTO costos2025 VALUES " + currentBatch.toString() + ";");
            }

            // Escribir el contenido procesado de nuevo al archivo original
            Files.write(Paths.get(filePath), String.join("\n", batchedInserts).getBytes(StandardCharsets.UTF_8));

            // Ejecutar los lotes en la base de datos
            for (String batch : batchedInserts) {
                jdbcTemplate.execute(batch); // Ejecuta cada lote de inserción
            }

            // Mensaje de éxito
            System.out.println(
                    "SQL file transformed into batched inserts (500 rows per batch) and executed. Changes saved to: "
                            + filePath);

        } catch (IOException e) {
            System.err.println("Error reading SQL file: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error processing SQL file: " + e.getMessage());
            throw e;
        }
    }
}
