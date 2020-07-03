package com.gabriel.core.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.annotation.processing.FilerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.gabriel.core.properties.FileStorageProperties;

@Service
public class FilesService implements IFilesService{
    
    private final Path fileStorageLocation;
    
    
    @Autowired
    public FilesService(FileStorageProperties fileStorageProperties) {
	//asignamos la direccion de el properties
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
          System.err.println(ex);
        }
    }


    @Override
    public String storeFile(MultipartFile file) {
	 // Normaliza el nombre del archivo
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // vefificamos si el nombre contiene caracteres invalidos
            if(fileName.contains("..")) {
                throw new FilerException("El nombre del archivo contiene caracteres invalidos " + fileName);
            }

            //copia el archivo a la ruta seleccionada(Remplazando el archivo con el mismo nombre si existe)
            Path targetLocation = fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            

            return fileName;
        } catch (IOException ex) {
           System.err.println(ex);
        }
	return fileName;
    }

}
