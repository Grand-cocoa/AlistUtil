package com.mavis.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration("97166c07-9e60-405b-adc7-897c142b4119")
@ConfigurationProperties(prefix = "alist")
public class AlistConfig {
    private String alistBaseUrl;
    private String alistUsername;
    private String alistPassword;

    private static final AlistConfig INSTANCE = new AlistConfig();

    public static void initialization(String alistBaseUrl, String alistUsername, String alistPassword){
        INSTANCE.alistBaseUrl = alistBaseUrl;
        INSTANCE.alistUsername = alistUsername;
        INSTANCE.alistPassword = alistPassword;
    }

    public static AlistConfig getConfig(){
        return INSTANCE;
    }

}
