package com.gabriel.core.services;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.FilerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.gabriel.core.ida.Dispersor;
import com.gabriel.core.ida.Recuperador;
import com.gabriel.core.properties.FileStorageProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Gabriel Lopez
 *Clase que se encarga de la administracion de los archivos
 */
@Service
@Slf4j
public class FilesService implements IFilesService {

    @Autowired
    private FileStorageProperties storage;

    @Autowired
    private IUtilsService utils;
    
    
    private final Path fileStorageLocation;
    private String fileName;

    @Autowired
    public FilesService(FileStorageProperties fileStorageProperties) {
	// asignamos la direccion de el properties
	this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();

	try {
	    Files.createDirectories(this.fileStorageLocation);
	} catch (Exception ex) {
	    log.error("Error al crear el directorio" + ex);
	}
    }

    /**
     * Metodo que se encarga de persistir un archivo en fileStoragelocation
     */
    @Override
    public String storeFile(MultipartFile file) {
	try {
	    // Normaliza el nombre del archivo
	    fileName = StringUtils.cleanPath(file.getOriginalFilename());
	    // vefificamos si el nombre contiene caracteres invalidos
	    if (fileName.contains("..")) {
		throw new FilerException("El nombre del archivo contiene caracteres invalidos " + fileName);
	    }

	    // copia el archivo a la ruta seleccionada(Remplazando el archivo con el mismo
	    // nombre si existe)
	    Path targetLocation = fileStorageLocation.resolve(fileName);
	    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

	    return fileName;
	} catch (IOException ex) {
	    log.error("Error en storeFile: " + ex);
	    return null;
	}
    }

    /**
     * Metodo que se encarga de cargar al servidor el archivo a dispersar y ejecutar el algoritmo de dispersion
     */
    @Override
    public boolean uploadFile(MultipartFile file) {

	if (storeFile(file) != null) {

	    if (idaDispersor()) {
		return true;
	    }
	    return false;
	} else {
	    return false;
	}
    }
    
    
    /**
     * Metodo que se encarga de cargar los archivos dispersos al servidor y ejecutar el algoritomo de recuperacion
     */
    @Override
    public boolean uploadDispersos(MultipartFile[] files) {
	try {
	    for(MultipartFile file: files) {
		    storeFile(file);
	}
	    idaRecuperador(files);
	    return true;
	}catch(Exception e) {
	    log.error("Error al cargar dispersos: "+e);
	    return false;
	}

    }

    /**
     * Metodo encargado de mandar a llamar el algoritmo de dispersion del IDA
     */
    @Override
    public boolean idaDispersor() {

	try {
	    String foto = fileName;
	    String uploadImg = storage.getUploadDir().concat(foto);
	    log.info("Comenzando dispercion de: " + uploadImg);
	    Dispersor disp = new Dispersor(storage.getLogsDir(),uploadImg, 100, 16, 3, 5, 69643, "KB");
	    disp.crearDispersos();
	    log.info("Dispersos creados satisfactoriamente");
	    return true;
	} catch (IOException e) {
	    log.info("Error al dispersar");
	    log.error("" + e);
	    return false;
	}

    }

    /**
     * Metodo encargado de mandar a llamar el alforitmo de recuperacion del ida
     */
    @Override
    public boolean idaRecuperador(MultipartFile[] files) {
	String processDir = storage.getGeneratedDir();
	String uploadDir = storage.getUploadDir();

	log.info("generando aleatorios para files tam: "+files.length);
	List<Integer> randList = new ArrayList<>(utils.randomUnRepeat(3,0,files.length -1));
	
	log.info("lista: "+randList.toString());
	log.info(files[randList.get(0)].getOriginalFilename());
	
	String[] nombreDispersos = {
		files[randList.get(0)].getOriginalFilename(),
		files[randList.get(1)].getOriginalFilename(),
		files[randList.get(2)].getOriginalFilename()
		};
	log.info("arreglo creado: {"+nombreDispersos[0]+","+nombreDispersos[1]+","+nombreDispersos[2]+"}"); 
	
	
	Recuperador rec = new Recuperador(storage.getLogsDir(),nombreDispersos, uploadDir, processDir, 100, 16, 69643, "KB");
	try {
	    rec.recuperarOriginal();
	} catch (IOException e) {
	    log.info("Error al recuperar archivo");
	    log.error("" + e);
	    return false;
	}
	return true;

    }

}
