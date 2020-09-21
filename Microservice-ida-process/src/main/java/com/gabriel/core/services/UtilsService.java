package com.gabriel.core.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;


/**
 * clase que proporciona algunos metodos utiles para los demas servicios, como filtrado de archivos, 
 * limpieza de directorios, codificacion en base64 etc.
 * @author Gabriel Lopez
 *
 */
@Service
@Slf4j
public class UtilsService implements IUtilsService {

    /**
     * lista los nombres de archivos del tipo especificado contenidos en una carpeta si el parametro es false,
     * lista los nombres de archivos diferentes al tipo especificado si el parametro in es true
     * @param path
     * @param mimeType
     * @param inv
     * @return
     */
    @Override
    public List<String> listaNameFiles(File file, String mimeType,boolean inv) {
	ArrayList<String> archivos = new ArrayList<>();
	System.out.println(file);

	try {
	    for (File ite : file.listFiles()) {
		System.out.println(ite);
		if(inv) {
		    System.out.println("inverso");
		    System.out.println("Tipo: "+Files.probeContentType(ite.toPath()));
		    if (Files.probeContentType(ite.toPath()) != null
				&& !(Files.probeContentType(ite.toPath()).equals(mimeType))) {
			    archivos.add(ite.getName());
			}
		}else {
		    System.out.println("normal");
		    System.out.println("Tipo: "+Files.probeContentType(ite.toPath()));
		    if (Files.probeContentType(ite.toPath()) != null
				&& Files.probeContentType(ite.toPath()).equals(mimeType)) {
			    archivos.add(ite.getName());
			}
		}
		
	    }
	    return archivos;
	} catch (IOException e) {
	    log.error("Error al cargar nomnbres de archivos dispersos: "+e);
	    return null;
	}

    }

    /**
     * devuelve una lista con los archivos que contengan un substring en su nombre
     */
    @Override
    public List<String> listaNameFilesContains(File file, String substring, boolean inv) {
	try {
	    ArrayList<String> archivos = new ArrayList<>();
	    for (File ite : file.listFiles()) {
		if(inv) {
		    if (ite != null && !(ite.getName().contains(substring))) {
			    archivos.add(ite.getName());
			}
		}else {
		    if (ite != null && ite.getName().contains(substring)) {
			System.out.println(ite.getName());
			    archivos.add(ite.getName());
			}
		}
		
	    }
	    return archivos;
	} catch (Exception e) {
	    log.error("Error al cargar la lista con nombres de los archivos dispersos");
	    return null;
	}
    }

    @Override
    public boolean limpiarCarpeta(File file) {
	try {
	    for(File ite: file.listFiles()) {
		    if(!ite.isDirectory()) {
			ite.delete();
		    }
		}
	    return true;
	}catch(Exception e) {
	    log.error("Error al limpiar carpeta: "+e);
	    return false;
	}
	
    }
    
    /**
     * Metodo que codifica en base64 bits de una imagen
     * @param file
     * @return
     * @throws Exception
     */
    public String encodeFileToBase64Binary(File file) throws Exception{
        FileInputStream fileInputStreamReader = new FileInputStream(file);
        byte[] bytes = new byte[(int)file.length()];
        fileInputStreamReader.read(bytes);
        fileInputStreamReader.close();
        return new String(Base64.getEncoder().encode(bytes), "UTF-8");
    }

    
    /**
     * Metodo que devuelve un una lista de numeros enteros aleatorios sin repetir de tamaño size y rango
     * floor and top
     * @param size
     * @return
     */
    @Override
    public Set<Integer> randomUnRepeat(int size,int floor,int top) {

	Set<Integer> randoms = new HashSet<>();
	
	while(randoms.size() < size) {
	    Integer rand = (int) Math.floor(Math.random()*(top-floor+1)+floor);
	    randoms.add(rand);
	}
	return randoms;
	
    }

    
    
}
