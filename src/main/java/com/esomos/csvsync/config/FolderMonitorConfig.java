package com.esomos.csvsync.config;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;


import org.springframework.context.annotation.Configuration;

import com.esomos.csvsync.service.CsvProcessorService;
import com.esomos.csvsync.service.DatabaseConnectionManager;

import jakarta.annotation.PostConstruct;

@Configuration
public class FolderMonitorConfig {

    private final CsvProcessorService csvProcessorService;
    private final DatabaseConnectionManager connectionManager;

    private final List<String> folderPaths = List.of(
            "C:\\Users\\TI\\Desktop\\mnto",
            "C:\\Users\\TI\\Desktop\\ti",
            "C:\\Users\\TI\\Desktop\\sgv"
    );

    public FolderMonitorConfig(CsvProcessorService csvProcessorService, DatabaseConnectionManager connectionManager) {
        this.csvProcessorService = csvProcessorService;
        this.connectionManager = connectionManager;
    }

    @PostConstruct
    public void init() throws Exception {
        startFolderMonitor();
   
    }

    public void startFolderMonitor() throws Exception {
        Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory(); // Memoria total asignada a la JVM
		long freeMemory = runtime.freeMemory();   // Memoria libre dentro de la JVM
		long usedMemory = totalMemory - freeMemory; // Memoria usada por la JVM

		System.out.println("========== Memory Usage ==========");
		System.out.println("Total Memory: " + (totalMemory / 1024 / 1024) + " MB");
		System.out.println("Free Memory: " + (freeMemory / 1024 / 1024) + " MB");
		System.out.println("Used Memory: " + (usedMemory / 1024 / 1024) + " MB");
		System.out.println("=================================");
        WatchService watchService = FileSystems.getDefault().newWatchService();

        for (String folderPath : folderPaths) {
            Path path = Paths.get(folderPath);
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        }

        WatchKey key;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    Path contextPath = (Path) event.context();
                    Path fullPath = ((Path) key.watchable()).resolve(contextPath);

                    System.out.println("Nuevo archivo: " + fullPath);
                    if (fullPath.toString().endsWith(".csv")) {
                        handleNewCsvFile(fullPath);
                    }
                }
            }
            key.reset();
        }
    }

    private void handleNewCsvFile(Path fullPath) {
        try {
            String folderPath = fullPath.getParent().toString();
            connectionManager.changeInstanceBasedOnFolder(folderPath);
            Map<String, String> dbInfo = connectionManager.getDatabaseInfo();
            System.out.println("Conexión actual: " + dbInfo);
            csvProcessorService.processCsv(fullPath.toString().toLowerCase());
        } catch (Exception e) {
            System.err.println("Error procesando el archivo: " + fullPath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}
