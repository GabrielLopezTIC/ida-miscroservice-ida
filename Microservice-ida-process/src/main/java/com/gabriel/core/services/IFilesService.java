package com.gabriel.core.services;

import org.springframework.web.multipart.MultipartFile;


public interface IFilesService {
    
    String storeFile(MultipartFile file);
    
    boolean uploadFile(MultipartFile file);
    
    boolean idaDispersor();
    
    boolean idaRecuperador(MultipartFile[] file);
    
    boolean uploadDispersos(MultipartFile[] file);
    
}
