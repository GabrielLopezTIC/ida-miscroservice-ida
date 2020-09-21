package com.gabriel.core.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gabriel.core.beans.DispersorResponse;
import com.gabriel.core.beans.RecuperadorResponse;
import com.gabriel.core.properties.FileStorageProperties;
import com.gabriel.core.services.IFilesService;
import com.gabriel.core.services.IUtilsService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/file")
public class FilesController {

    @Autowired
    private IFilesService filesService;
    @Autowired
    private IUtilsService utils;
    @Autowired
    private FileStorageProperties storage;

    String fileName;

    @ApiOperation(value = "Cargar imagen", notes = "Permite cargar la imagen que se dispersara")
    @CrossOrigin(origins = "*")
    @PostMapping("/uploadAndProcessImg")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
	utils.limpiarCarpeta(new File(storage.getUploadDir()));
	utils.limpiarCarpeta(new File(storage.getGeneratedDir()));
	System.out.println("Cargando archivos");
	if (filesService.uploadFile(file)) {
	    return ResponseEntity.status(HttpStatus.OK).body(new DispersorResponse(
		    utils.listaNameFilesContains(new File(storage.getGeneratedDir()), "png.", false)));
	} else {
	    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontraron archivos cargados");
	}
    }

    @ApiOperation(value = "Cargar dispersos", notes = "Permite cargar los dispersos a partir de los cuales se recuperara la imagen")
    @CrossOrigin(origins = "*")
    @PostMapping("/uploadAndProcessDispersos")
    public ResponseEntity<?> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) throws Exception {
	utils.limpiarCarpeta(new File(storage.getUploadDir()));
	utils.limpiarCarpeta(new File(storage.getGeneratedDir()));
	
	if(files.length < 3)
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	    
	if (filesService.uploadDispersos(files)) {
	    List<String> imgName = utils.listaNameFilesContains(new File(storage.getGeneratedDir()),".png",false);
	    String img64 = utils.encodeFileToBase64Binary(
		    new File(storage.getGeneratedDir().concat(imgName.get(0))));
	    return ResponseEntity.status(HttpStatus.OK).body(new RecuperadorResponse(img64));
	} else {
	    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}

    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value="/zip", produces="application/zip")
    public void zipFiles(HttpServletResponse response) throws IOException {

        //setting headers  
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"Ida.zip\"");

        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());

        // create a list to add files to be zipped
        //ArrayList<File> files = new ArrayList<>(2);
        //files.add(new File("README.md"));

        String fileBasePath = storage.getGeneratedDir();
    	File files = new File(fileBasePath);
        for (File file : files.listFiles()) {          
            zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
            FileInputStream fileInputStream = new FileInputStream(file);
            IOUtils.copy(fileInputStream, zipOutputStream);
            fileInputStream.close();
            zipOutputStream.closeEntry();
        }    

        zipOutputStream.close();
    }
    

    
    
    
}
