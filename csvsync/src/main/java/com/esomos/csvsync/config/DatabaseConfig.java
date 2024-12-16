package com.esomos.csvsync.config;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;



@Setter
@Getter
@AllArgsConstructor
public class DatabaseConfig {
   
    private  String host;
    private  String port;
    private  String username;
    private  String password;
    private  String database;

   
}
