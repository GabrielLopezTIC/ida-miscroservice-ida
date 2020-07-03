package com.gabriel.core.services;

import org.springframework.web.multipart.MultipartFile;

public interface IFilesService {
    
    String storeFile(MultipartFile file);
    
}
