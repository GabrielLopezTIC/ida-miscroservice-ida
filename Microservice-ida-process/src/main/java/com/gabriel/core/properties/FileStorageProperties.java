package com.gabriel.core.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "location")
public class FileStorageProperties {
    
    private String uploadDir;
    private String generatedDir;
    private String logsDir;
    
    
    public String getUploadDir() {
        return uploadDir;
    }
    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
    public String getGeneratedDir() {
        return generatedDir;
    }
    public void setGeneratedDir(String generatedDir) {
        this.generatedDir = generatedDir;
    }
    public String getLogsDir() {
        return logsDir;
    }
    public void setLogsDir(String logsDir) {
        this.logsDir = logsDir;
    }
   
}
