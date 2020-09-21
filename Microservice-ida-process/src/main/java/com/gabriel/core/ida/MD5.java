package com.gabriel.core.ida;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.security.MessageDigest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumSet;

public class MD5 {

    private String separadorFileSystem = File.separator;

    public String calcularMD5(String ruta, int tamanioBuffer, String hashType) throws IOException, NoSuchAlgorithmException {

        StringBuilder sb = new StringBuilder();
        Path pathArchivoOriginal = Paths.get(ruta);
        //read a file using SeekableByteChannel
        SeekableByteChannel seekableByteChannel = Files.newByteChannel(pathArchivoOriginal, EnumSet.of(StandardOpenOption.READ));
        ByteBuffer byteBuffer = ByteBuffer.allocate(tamanioBuffer);
        //String encoding = System.getProperty("file.encoding");
        byteBuffer.clear();
        //System.out.println("tamanioBuffer: " + byteBuffer.array().length);
        //System.out.println("seekableByteChannel.size() vale: " + seekableByteChannel.size());
        MessageDigest md = MessageDigest.getInstance(hashType);

        int bytesLeidos = 0;
        while ((bytesLeidos = seekableByteChannel.read(byteBuffer)) > 0) {
            //System.out.println("bytesLeidos realmente:  " + bytesLeidos);
            byteBuffer.flip();
            //System.out.println("el position está en: " + byteBuffer.position());
            //System.out.println("el limit está en la posición: " + byteBuffer.limit());

            if (bytesLeidos < tamanioBuffer) {//quedan posiciones sin ocupar en el byteBuffer
                byte[] byteArrayRestante = new byte[byteBuffer.limit()];
                byte[] arrayByteBuffer = byteBuffer.array();
                //Con el siguiente ciclo eliminamos los espacios en blanco del byteBuffer
                for (int indice = 0; indice < byteBuffer.limit(); indice++) {
                    byteArrayRestante[indice] = arrayByteBuffer[indice];
                }
                md.update(byteArrayRestante); //Se actualiza el message digest
            } else {
                md.update(byteBuffer);
            }

            byteBuffer.clear();
        }
        byte[] array = md.digest();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    public ArrayList<String> obtenerListaCarpetasOArchivosOriginales(String ruta) throws IOException {
        ArrayList<String> arrayListArchOCarpetas = new ArrayList<>();
        Path pathArchivoOriginal = Paths.get(ruta);

        DirectoryStream<Path> ds = Files.newDirectoryStream(pathArchivoOriginal);
        for (Path file : ds) {
            arrayListArchOCarpetas.add(file.getParent() + separadorFileSystem + file.getFileName());
        }
        return arrayListArchOCarpetas;
    }
}
