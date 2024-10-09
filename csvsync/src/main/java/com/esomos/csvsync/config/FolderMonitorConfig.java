package com.esomos.csvsync.config;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import org.springframework.context.annotation.Configuration;

import com.esomos.csvsync.service.CsvProcessorService;

import jakarta.annotation.PostConstruct;

    
@Configuration
public class FolderMonitorConfig {

    private final CsvProcessorService csvProcessorService;

    public FolderMonitorConfig(CsvProcessorService csvProcessorService) {
        this.csvProcessorService = csvProcessorService;
    }

    @PostConstruct
    public void init() throws Exception {
        startFolderMonitor();
    }

    public void startFolderMonitor() throws Exception {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = FileSystems.getDefault().getPath("C:\\Users\\TI\\Desktop\\test");
        path.register(watchService, java.nio.file.StandardWatchEventKinds.ENTRY_CREATE);
        WatchKey key;

        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == java.nio.file.StandardWatchEventKinds.ENTRY_CREATE) {
                    System.out.println("new file: " + event.context().toString());
                    String fileName = event.context().toString();
                    if (fileName.endsWith(".csv")) {
                        csvProcessorService.processCsv(Paths.get(path.toString(), fileName).toString());
                    }
                }
            }
            key.reset();
        }
    }
}

