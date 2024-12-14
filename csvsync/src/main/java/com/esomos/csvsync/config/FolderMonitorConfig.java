package com.esomos.csvsync.config;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.esomos.csvsync.service.CsvProcessorService;

import jakarta.annotation.PostConstruct;

@Configuration
public class FolderMonitorConfig {

    @Autowired
    private CsvProcessorService csvProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate; // Inyectamos JdbcTemplate

    List<String> folderPaths = List.of("C:\\Users\\TI\\Desktop\\mnto",
            "C:\\Users\\TI\\Desktop\\ti",
            "C:\\Users\\TI\\Desktop\\sgv");

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
                        // Cambiar la base de datos según la carpeta
                        changeInstanceBasedOnFolder(fullPath.getParent().toString());
                        Map<String, String> dbInfo = getDatabaseInfo();
                        System.out.println("Conexión actual: " + dbInfo);
                        csvProcessorService.processCsv(fullPath.toString().toLowerCase());
                    }
                }
            }
            key.reset();
        }
    }

    public void changeInstanceBasedOnFolder(String folderPath) {
        String dbName = "";

        // Asignar la base de datos según la carpeta
        if (folderPath.contains("mnto")) {
            dbName = "mnto";
            changeDBconnection("localhost", "5433", "mnto", "1234", "postgres");
        } else if (folderPath.contains("ti")) {
            dbName = "ti";
            changeDBconnection("localhost", "5434", "ti", "1234", "postgres");
        } else if (folderPath.contains("sgv")) {
            dbName = "sgv";
            changeDBconnection("localhost", "5435", "sgv", "1234", "postgres");
        }

        System.out.println("Conexión cambiada a la base de datos: " + dbName);
    }

    public void changeDBconnection(String host, String port, String username, String password, String database) {
        String dbUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // Establecer el DataSource en el JdbcTemplate
        jdbcTemplate.setDataSource(dataSource);
    }

    public Map<String, String> getDatabaseInfo() {
        String sql = "SELECT current_user AS user, "
                + "inet_server_port() AS port, "
                + "current_database() AS db";

        // Ejecutamos la consulta
        Map<String, Object> result = jdbcTemplate.queryForMap(sql);

        // Convertimos el resultado a un Map de String
        Map<String, String> dbInfo = new HashMap<>();
        dbInfo.put("user", result.get("user").toString());
        dbInfo.put("port", result.get("port").toString());
        dbInfo.put("db", result.get("db").toString());

        return dbInfo;
    }

}
