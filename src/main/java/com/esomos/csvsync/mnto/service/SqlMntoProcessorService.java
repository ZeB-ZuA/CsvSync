package com.esomos.csvsync.mnto.service;

import org.springframework.stereotype.Service;

import com.esomos.csvsync.service.DataBaseService;

@Service
public class SqlMntoProcessorService {

    private final DataBaseService dataBaseService;

    public SqlMntoProcessorService(DataBaseService dataBaseService) {
        this.dataBaseService = dataBaseService;
    }


    public void procesMntoSql(String filePath) throws Exception {
        
    }
    
}
