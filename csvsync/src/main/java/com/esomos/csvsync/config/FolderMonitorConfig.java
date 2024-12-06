package com.esomos.csvsync.config;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.esomos.csvsync.service.CsvProcessorService;

import jakarta.annotation.PostConstruct;

@Configuration
public class FolderMonitorConfig {

    @Autowired
    private final CsvProcessorService csvProcessorService;

    List<String> folderPaths = List.of("C:\\Users\\TI\\Desktop\\mnto",
            "C:\\Users\\TI\\Desktop\\ti",
            "\\Users\\TI\\Desktop\\sgv");

    public FolderMonitorConfig(CsvProcessorService csvProcessorService) {
        this.csvProcessorService = csvProcessorService;
    }

    @PostConstruct
    public void init() throws Exception {
        startFolderMonitor();
    }

    public void startFolderMonitor() throws Exception {
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

                    System.out.println("Nuevo archivo: " + fullPath.toString());
                    String fileName = fullPath.toString();
                    if (fileName.endsWith(".csv")) {
                        csvProcessorService.processCsv(fullPath.toString().toLowerCase());
                    }
                }
            }
            key.reset();
        }
    }
}
