package com.esomos.csvsync.SqlUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

public class SqlUtils {
    public static void executeSqlFileInBatches(String filePath, JdbcTemplate jdbcTemplate) throws IOException {
        try {
            String content = Files.readString(Paths.get(filePath), Charset.forName("Windows-1252"));
    
            String[] sqlStatements = content.split(";");
    
            List<String> batchedInserts = new ArrayList<>();
            StringBuilder currentBatch = new StringBuilder();
            int batchCount = 0;
            int totalBatches = 0;
    
            String columns = "(num_ot, bus, fecha_origen, status_ot, fecha_cierre, area, especialidad, tipo_reporte, trab_requerido, cod_falla, cod_accion, cod_causa, kms, ot_trab_realizado, cod_actividad, act_tiporeporte, act_trab_requerido, num_rm, codigo_item_srv, desc_item_srv, cant_usada, udm, costo_unitario, subtotal, fecha_uso, num_rt, rt_area, rt_especialidad, rt_trab_requerido, rt_trab_realizado, planeador_mtlrs, ubicacion, ref_id, especialidad_rec)";
    
            for (String sql : sqlStatements) {
                sql = sql.trim();
                if (sql.startsWith("INSERT INTO") && !sql.isEmpty()) {
                    int valuesStartIndex = sql.indexOf("VALUES") + 6; 
                    String valuesPart = sql.substring(valuesStartIndex).trim();
    
                    if (!valuesPart.startsWith("(")) {
                        valuesPart = "(" + valuesPart;
                    }
                    if (!valuesPart.endsWith(")")) {
                        valuesPart = valuesPart + ")";
                    }
    
                    if (batchCount < 500) {
                        if (currentBatch.length() > 0) {
                            currentBatch.append(", ");
                        }
                        currentBatch.append(valuesPart);
                        batchCount++;
                    } else {
                        // Construir un INSERT batch con ON CONFLICT DO NOTHING
                        batchedInserts.add("INSERT INTO costos2025 " + columns + " VALUES " + currentBatch.toString() + " ON CONFLICT (ref_id) DO NOTHING;");
                        currentBatch.setLength(0);
                        currentBatch.append(valuesPart);
                        batchCount = 1;
                        totalBatches++;
                    }
                }
            }
    
            // Agregar el último batch pendiente
            if (currentBatch.length() > 0) {
                batchedInserts.add("INSERT INTO costos2025 " + columns + " VALUES " + currentBatch.toString() + " ON CONFLICT (ref_id) DO NOTHING;");
                totalBatches++;
            }
    
            System.out.println("Total de lotes a procesar: " + totalBatches);
    
            int currentBatchIndex = 0;
            for (String batchInsert : batchedInserts) {
                jdbcTemplate.execute(batchInsert);
                currentBatchIndex++;
                System.out.println("Lote " + currentBatchIndex + " de " + totalBatches + " procesado.");
            }
    
            jdbcTemplate.execute("SELECT update_ubicacion2_and_tipo_before_insert_fuc();");
            System.out.println("Inserciones completas.");
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    
}
