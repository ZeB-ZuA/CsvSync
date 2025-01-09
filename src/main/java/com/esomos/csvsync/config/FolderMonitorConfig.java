package com.esomos.csvsync.config;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Configuration;

import com.esomos.csvsync.SqlUtils.SqlUtils;
import com.esomos.csvsync.mnto.service.SqlMntoProcessorService;
import com.esomos.csvsync.service.CsvProcessorService;
import com.esomos.csvsync.service.DatabaseConnectionManager;
import com.esomos.csvsync.service.SqlProcessorService;

import jakarta.annotation.PostConstruct;

@Configuration
public class FolderMonitorConfig {

    private final CsvProcessorService csvProcessorService;
    private final SqlProcessorService sqlProcessorService;
    private final SqlMntoProcessorService sqlMntoProcessorService;
    private final DatabaseConnectionManager DatabaseConnectionManager;

    private final List<String> folderPaths = List.of(
            "C:\\Users\\TI\\Desktop\\mnto",
            "C:\\Users\\TI\\Desktop\\ti",
            "C:\\Users\\TI\\Desktop\\sgv",
            "C:\\Users\\TI\\Desktop\\mnto\\Costos2025");

    public FolderMonitorConfig(CsvProcessorService csvProcessorService, DatabaseConnectionManager connectionManager,
            SqlProcessorService sqlProcessorService, SqlMntoProcessorService sqlMntoProcessorService) {
        this.csvProcessorService = csvProcessorService;
        this.DatabaseConnectionManager = connectionManager;
        this.sqlProcessorService = sqlProcessorService;
        this.sqlMntoProcessorService = sqlMntoProcessorService;
    }

    @PostConstruct
    public void init() throws Exception {
        startFolderMonitor();

    }

    public void startFolderMonitor() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        System.out.println("========== Memory Usage ==========");
        System.out.println("Total Memory: " + (totalMemory / 1024 / 1024) + " MB");
        System.out.println("Free Memory: " + (freeMemory / 1024 / 1024) + " MB");
        System.out.println("Used Memory: " + (usedMemory / 1024 / 1024) + " MB");
        System.out.println("=================================");

        WatchService watchService = FileSystems.getDefault().newWatchService();

        // Registrar todos los directorios para monitoreo
        for (String folderPath : folderPaths) {
            Path path = Paths.get(folderPath);
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        }

        WatchKey key;
        while (true) { // Bucle infinito para monitorear eventos
            key = watchService.take(); // Espera un evento

            if (key != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path contextPath = (Path) event.context();
                        Path fullPath = ((Path) key.watchable()).resolve(contextPath);

                        System.out.println("Nuevo archivo: " + fullPath);

                        if (fullPath.toString().endsWith(".csv")) {
                            handleNewCsvFile(fullPath);
                        }
                        if (fullPath.toString().endsWith(".sql")) {
                            handleNewSqlFile(fullPath);
                        }
                    }
                }

                // Restablecer el key para permitir que WatchService siga monitoreando
                key.reset();

                // Agregar un pequeño retraso para evitar la sobrecarga y permitir la detección
                // de otros archivos
                try {
                    Thread.sleep(500); // 500 milisegundos de espera entre cada ciclo
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleNewSqlFile(Path fullPath) {
        try {
            String folderPath = fullPath.getParent().toString();

            if (folderPath.endsWith("Costos2025")) {
                System.out.println("Archivo detectado en la carpeta 'Costos2025': " + fullPath);

                DatabaseConnectionManager.changeDBConnection("localhost", "5433", "mnto", "1234", "presupuesto");
                Map<String, String> dbInfo = DatabaseConnectionManager.getDatabaseInfo();
                System.out.println("Conexión actualizada: " + dbInfo);

                sqlMntoProcessorService.procesMntoSql(fullPath.toString());

                SqlUtils.executeSqlFileInBatches(fullPath.toString(), DatabaseConnectionManager.getJdbcTemplate());
            } else {
                // Si el archivo no está en la carpeta "Costos2025", se podría registrar
                System.out.println("Archivo .sql detectado fuera de 'Costos2025': " + fullPath);
            }
        } catch (Exception e) {
            System.err.println("Error procesando el archivo SQL: " + fullPath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleNewCsvFile(Path fullPath) {
        try {
            String folderPath = fullPath.getParent().toString();
            DatabaseConnectionManager.changeInstanceBasedOnFolder(folderPath);
            Map<String, String> dbInfo = DatabaseConnectionManager.getDatabaseInfo();
            System.out.println("Current connection: " + dbInfo);
            csvProcessorService.processCsv(fullPath.toString().toLowerCase());
        } catch (Exception e) {
            System.err.println("Error procesando el archivo: " + fullPath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}
