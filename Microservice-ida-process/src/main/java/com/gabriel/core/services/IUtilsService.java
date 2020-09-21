package com.gabriel.core.services;

import java.io.File;
import java.util.List;
import java.util.Set;

public interface IUtilsService {
    
    /**
     * lista los nombres de archivos del tipo especificado contenidos en una carpeta si el parametro es false,
     * lista los nombres de archivos diferentes al tipo especificado si el parametro in es true
     * @param path
     * @param mimeType
     * @param inv
     * @return
     */
    List<String> listaNameFiles(File file,String mimeType,boolean inv);
    
    /**
     * si el parametro es true lista los nombres de archivos que contengan en su nombre la cadena
     * especificada y que esten contenidos en una carpeta,
     *  si el parametro es false
     * lista los nombres que no contengan la cadena especificado 
     * @param path
     * @param mimeType
     * @param inv
     * @return
     */
    List<String> listaNameFilesContains(File file, String substring ,boolean inv);
    
    /**
     * Funcion que elimina todos los archivos de una carpeta
     * @param file
     * @return
     */
    boolean limpiarCarpeta(File file);
    
    /**
     * Metodo que codifica en base64 bits de una imagen
     * @param file
     * @return
     * @throws Exception
     */
    String encodeFileToBase64Binary(File file) throws Exception;
    
    /**
     * Metodo que devuelve un una lista de numeros enteros aleatorios sin repetir de tamaño size y rango
     * floor and top
     * @param size
     * @return
     */
    Set<Integer> randomUnRepeat(int size,int floor, int top);
    
    
}
