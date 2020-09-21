package com.gabriel.core.ida;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;



//cambios añadidos para integrar con spring properties: atributo  rutaLogs añadido y agregado a contructor
public class GRIDA {

    private int campo;
    private int polinomioPrimitivo = 69643;
    private int logaritmo[];
    private int antiLogaritmo[];
    private int ordenCampo;
    
    
    private String rutaLogs;

    public GRIDA(String rutaLogs,int ordenCampo) {
	this.rutaLogs = rutaLogs;
	
        this.ordenCampo = ordenCampo;
        this.campo = (int) Math.pow(2, ordenCampo) - 1;//(2^16)-1 es el valor que se asigna a esta variable
    }

    public int[] getLogaritmo() {
        return logaritmo;
    }

    /*public void setLogaritmo(int[] logaritmo) {
        this.logaritmo = logaritmo;
    }*/
    public int[] getAntiLogaritmo() {
        return antiLogaritmo;
    }

    /*public void setAntiLogaritmo(int[] antiLogaritmo) {
        this.antiLogaritmo = antiLogaritmo;
    }*/
    /**
     * Este método se utiliza para crear la matriz de Vandermonde que se
     * requiere para llevar a cabo la dispersión.
     */
    public int[][] generarMatrizVandermonde() {
        int matrizVandermonde[][] = {{1, 1, 1},
        {1, 2, 4},
        {1, 3, 9},
        {1, 4, 16},
        {1, 5, 25}};

        return matrizVandermonde;
    }

    /**
     * Este método permite generar los arreglos de logaritmo y antilogaritmo
     * necesarios para la creación de dispersos.
     *
     *
     * @param ordenCampo el orden del campo finito.
     */
    public void generarLogYAntiLog() throws IOException {
        
        //String logFile = "log.txt";
        //String antiLogFile = "antilog.txt";
	//System.out.println("LA ruta es :  "+rutaLogs);
        //String logFile = "\\Users\\Yareni\\Desktop\\ida-storage\\logsFiles\\log.txt";
        //String antiLogFile = "\\Users\\Yareni\\Desktop\\ida-storage\\logsFiles\\antilog.txt";
	
	String logFile = rutaLogs.concat("log.txt");
	String antiLogFile = rutaLogs.concat("antiLog.txt");
	
        
        Reader reader = Files.newBufferedReader(Paths.get(logFile));
        logaritmo = obtenerArreglo(reader);
        
        System.out.println("logaritmo primer valor es: "+logaritmo[0]);
        System.out.println("logaritmo penultimo valor es: "+logaritmo[65534]);
        
        reader = Files.newBufferedReader(Paths.get(antiLogFile));
        antiLogaritmo = obtenerArreglo(reader);
        
        System.out.println("logaritmo primer valor es: "+antiLogaritmo[0]);
        System.out.println("logaritmo penultimo valor es: "+antiLogaritmo[65534]);
       

    }

    public int[] obtenerArreglo(Reader reader) throws IOException {

        int[] arreglo = new int[65536];
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);

        //El siguiente for saca un renglón completo.
        int contadorArreglo = 0;
        for (CSVRecord csvRecord : csvParser) {
            //System.out.println("Record No - " + csvRecord.getRecordNumber());
            // Accediendo a los valores por el indice de la columna
            //El siguiente for obtendrá los 100 registros de cada renglón.
            if (csvRecord.getRecordNumber() != 656) {
                for (int i = 0; i < 100; i++) {
                    //System.out.print(" "+csvRecord.get(i));
                    arreglo[contadorArreglo] = Integer.parseInt(csvRecord.get(i).trim());
                    contadorArreglo++;
                }
            } else if (csvRecord.getRecordNumber() == 656) {
                //System.out.println("Ya entramos al ultimo renglon");
                for (int i = 0; i < 36; i++) {
                    arreglo[contadorArreglo] = Integer.parseInt(csvRecord.get(i).trim());
                    contadorArreglo++;
                }
            }
        }//fin del for

        return arreglo;
    }

}
