package com.gabriel.core.ida;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;*/
/**
 * *
 * Clase que contiene los métodos necesarios para llevar a cabo la dispersión de
 * un archivo.
 *
 * @author Jorge
 */

//cambios añadidos para integrar con spring properties: atributo  rutaLogs añadido y agregado a contructor
public class Dispersor { //implements Runnable {

    //private static Logger logaritmoger = LogManager.getLogger(Dispersor.class.getName());
    private int[] antiLogaritmo;
    private int[] logaritmo;

    private int matrizVandermonde[][];

    private String ruta;
    private double porcentajeAcumulado, porcentajeIndividual;
    private long tamanioArchivo = 0;
    private String separadorFileSystem = File.separator;
    private int tamanioBuffer;
    @SuppressWarnings("unused")
    private int m, n, ordenCampo, polinomioPrimitivo, par, campo;

    private String rutaLogs;
    
    public Dispersor(String rutaLogs,String ruta, int tamanioBuffer, int ordenCampo, int m, int n, int polinomioPrimitivo, String unidad) {
       
	this.rutaLogs = rutaLogs;
	
	this.ruta = ruta;
        this.ordenCampo = ordenCampo;
        this.m = m;
        this.n = n;
        this.polinomioPrimitivo = polinomioPrimitivo;
        this.campo = (int) Math.pow(2, ordenCampo) - 1;

        if (unidad.equals("MB")) {
            this.tamanioBuffer = (tamanioBuffer * 1024 * 1024) * m * 2;//en MB, se multiplica por m para que sea múltiplo de los dispersos y * 2 para la conversion de shorts.
            System.out.println("el buffer en MB vale: " + this.tamanioBuffer);
        }

        if (unidad.equals("KB")) {
            this.tamanioBuffer = (tamanioBuffer * 1024) * m * 2;//en MB, se multiplica por m para que sea múltiplo de los dispersos y * 2 para la conversion de shorts.
            System.out.println("el buffer en KB vale: " + this.tamanioBuffer);
        }
    }
    
    
    
    
    

    // @Override
    /* public void run() {
        crearDispersos(ruta);
    }*/
    public int sumar(int operando1, int operando2) {
        return (operando1 ^ operando2);
    }

    public int multiplicar(int operando1, int operando2) {
        if ((operando1 != 0) && (operando2 != 0)) {
            return (logaritmo[(antiLogaritmo[operando1] + antiLogaritmo[operando2]) % campo]);
        } else {
            return 0;
        }
    }

    /**
     * *
     * Método mediante el cual se lleva a cabo los pasos para crear los
     * dispersos de un archivo.
     *
     * @param ruta la ruta donde se encuentra el archivo que se desea dispersar.
     */
    //public void crearDispersos(String ruta) {
    public void crearDispersos() throws IOException {
        GRIDA grida = new GRIDA(rutaLogs,16);

        matrizVandermonde = grida.generarMatrizVandermonde();
        grida.generarLogYAntiLog();
        
        logaritmo = grida.getLogaritmo();
        antiLogaritmo = grida.getAntiLogaritmo();

        // codigo original
        //String nombreArchivo = separarNombreArchivo(ruta, separadorFileSystem);
        //String rutaDispersos = separarRuta(ruta, separadorFileSystem);
        
        //codigo modificado
        String nombreArchivo = separarNombreArchivo(ruta, separadorFileSystem);
        String rutaDispersos = separarRuta("\\Users\\Yareni\\Desktop\\ida-storage\\destinoFiles\\", separadorFileSystem);

        /*logger.trace("nombreArchivo es: " + nombreArchivo);
        logger.trace("rutaDispersos: " + rutaDispersos);*/
        String nombreArchivoOriginal = obtenerNombreArchivo(ruta, separadorFileSystem);
        Path pathArchivoOriginal = Paths.get(rutaDispersos, nombreArchivoOriginal);
        LocalDateTime ldt = LocalDateTime.now();
        Timestamp ts = Timestamp.valueOf(ldt);
        
        

        //logger.trace("Iniciamos en: " + ts);
        int temporal = 0;

        try {

            File fichero = new File(pathArchivoOriginal.toString());
            tamanioArchivo = fichero.length(); //Obtengo el tamanio de un disperso
            

            //logger.trace("tamanioArchivo es: " + tamanioArchivo);
            //asignarPorcentajeIndividual();
            //TamanioArchivo/2 se debe a que vamos a estar siempre operando sobre
            // 16 bits, esto hace que realmente el tamaño se reduzca a la mitad
            //y se sabrá con más certeza si hay o no residuos a tomar en cuenta para la recuperación.
            long bytesUltimoChunk = tamanioArchivo % tamanioBuffer;//Estamos calculando el # bytes restantes cuando leidos < tamanioBuffer
            par = (int) bytesUltimoChunk % 2;// 0 es par y 1 es impar, el 1 implica convertir el último byte a un short

            int residuo = (int) Math.ceil(bytesUltimoChunk / 2.0) % m;// el /2 es por el uso de shorts
            System.out.println("residuo vale: " + residuo);//Me sirve para calcular el relleno
            //logger.trace("residuo para la dispersión vale: " + residuo);

            String nombresDispersos[] = new String[n];
            SeekableByteChannel seekBChannels[] = new SeekableByteChannel[n];
            ShortBuffer shortBufferParamDisp[] = new ShortBuffer[n];

            /**
             * En el siguiente bloque for se crearán los n dispersos donde se
             * guardarán tanto el residuo, m, n, par y el renglón de la matriz
             * que utilizaron.
             *
             */
            for (int i = 0; i < n; i++) {
                nombresDispersos[i] = rutaDispersos + separadorFileSystem + nombreArchivo + "." + i;
                try {
                    seekBChannels[i] = Files.newByteChannel(Paths.get(nombresDispersos[i]), EnumSet.of(StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE));
                } catch (NoSuchFileException nsfe) {
                    seekBChannels[i] = Files.newByteChannel(Paths.get(nombresDispersos[i]), EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE));
                }
                shortBufferParamDisp[i] = ShortBuffer.allocate(4 + m);

                shortBufferParamDisp[i].put((short) residuo); //Para calcular el relleno
                shortBufferParamDisp[i].put((short) n);
                shortBufferParamDisp[i].put((short) m);
                shortBufferParamDisp[i].put((short) par);

                for (int j = 0; j < m; j++) {
                    shortBufferParamDisp[i].put((short) matrizVandermonde[i][j]);
                }
                shortBufferParamDisp[i].flip();

                ByteBuffer byteBuffer = ByteBuffer.allocate((shortBufferParamDisp[i].capacity()) * 2);
                for (int indice = 0; indice < shortBufferParamDisp[i].capacity(); indice++) {
                    byteBuffer.putShort(shortBufferParamDisp[i].get(indice));
                }
                byteBuffer.flip();
                //A continuación procedemos a escribir los parámetros de los dispersos en disco duro.
                seekBChannels[i].write(byteBuffer);
            }
            System.out.println("Ya terminamos de escribir en los archivos salientes");

            //A continuación creamos todos los arreglos que serán utilizados para guardar
            //la información de los dispersos
            //logger.trace("tamanioArchivo / 3 vale: " + (tamanioArchivo / 3.0));
            SeekableByteChannel seekableByteChannel = Files.newByteChannel(pathArchivoOriginal, EnumSet.of(StandardOpenOption.READ));

            ByteBuffer byteBuffer = ByteBuffer.allocate(tamanioBuffer);

            byteBuffer.clear();
           

            int bytesLeidos = 0;

            while ((bytesLeidos = seekableByteChannel.read(byteBuffer)) > 0) {
                System.out.println("acabamos de leer: " + bytesLeidos + " bytes");

                byteBuffer.flip();
                ShortBuffer shortBuffer = null;
                int[] nextShort = new int[m];

                //En la siguiente línea, convertimos los bytes a shorts y se reduce el tamaño
                //de datos a la mitad.
                if (bytesLeidos < tamanioBuffer) {//quedan posiciones sin ocupar en el byteBuffer 
                    if (bytesLeidos == 1) {
                        //System.out.println("Entramos a if(bytesLeidos == 1){");
                        shortBuffer = ShortBuffer.allocate(1);
                        shortBuffer.put(byteBuffer.get());
                    }
                    if (par == 0) {//es par y es igual a bytesLeidos%2 == 0
                        //System.out.println("Entramos en  if(par == 0){");
                        shortBuffer = byteBuffer.asShortBuffer();
                    }
                    if (par == 1 && bytesLeidos > 2) { //es impar y hay varios bytes restantes
                        //Al ser impar a la siguiente línea le faltará convertir el último byte en short
                        //System.out.println("Entramos en if(par == 1 && bytesLeidos > 2)");
                        ShortBuffer shortBuffTemp = byteBuffer.asShortBuffer();

                        /*System.out.println("shortBuffTemp.capacity(): "+shortBuffTemp.capacity());
                        System.out.println("shortBuffTemp.limit(): "+shortBuffTemp.limit());
                        System.out.println("shortBuffTemp.position(): "+shortBuffTemp.position());*/
                        shortBuffer = ShortBuffer.allocate(shortBuffTemp.capacity() + 1);
                        shortBuffer.put(shortBuffTemp);

                        /*System.out.println("byteBuffer.capacity() es: " + byteBuffer.capacity());
                        System.out.println("byteBuffer.limit() es: "+byteBuffer.limit());
                        System.out.println("byteBuffer.position() es: "+byteBuffer.position());*/
                        //shortBuffer.put(byteBuffer.get(((int) Math.ceil(byteBuffer.capacity() / 2.0)) - 1));
                        shortBuffer.put(byteBuffer.get(byteBuffer.limit() - 1));
                    }
                } else {// si bytesLeidos es igual a tamanioBuffer
                    shortBuffer = byteBuffer.asShortBuffer();
                }

                //System.out.println("shortBuffer.capacity() vale: " + shortBuffer.capacity());
                int[] arregloTodoPositivo = new int[shortBuffer.capacity()];

                for (int indice = 0; indice < arregloTodoPositivo.length; indice++) {
                    short valorShort = shortBuffer.get(indice);
                    //System.out.println("valorShort vale: " + valorShort);
                    if (valorShort < 0) {//Si el contenido es negativo
                        int enteroPositivo = valorShort & (0xffff); //--> Con esta operación podemos convertir un short a entero positivo
                        arregloTodoPositivo[indice] = enteroPositivo;
                        //System.out.println("convertido en positivo vale: " + enteroPositivo);
                    } else {
                        arregloTodoPositivo[indice] = valorShort;
                        //System.out.println("ya era positivo y vale: " + valorShort);
                    }
                }
                double tamanioArreglo = 0.0;

                tamanioArreglo = Math.ceil((double) arregloTodoPositivo.length / (double) m);
                //logger.trace("tamanioArreglo para los dispersos vale: " + tamanioArreglo);

                //A continuación se crea un arrayList de arreglos de ints para guardar los datos de los dispersos en memoria.
                ArrayList<int[]> arrayListInfoDispersos = new ArrayList<>();

                for (int i = 0; i < n; i++) {
                    arrayListInfoDispersos.add(new int[(int) tamanioArreglo]);
                }

                int indiceArreglos = 0;

                //for (int contadorTamanio = 0; contadorTamanio < tamanioArchivo;) {
                //Se debe recorrer el pedazo del archivo original para irlo convirtiendo
                for (int contadorTamanio = 0; contadorTamanio < arregloTodoPositivo.length;) {
                    //Se procede a recolectar m shorts

                    for (int indice = 0; indice < nextShort.length; indice++) {

                        if (contadorTamanio < arregloTodoPositivo.length) {
                            nextShort[indice] = arregloTodoPositivo[contadorTamanio];
                            //actualizarProgreso();
                            contadorTamanio++;
                        } else {
                            /*System.out.println("Estoy en el else de la recolección de los m shorts e indice vale: " + indice);
                            System.out.println("contadorTamanio vale: " + contadorTamanio);*/
                            break;
                        }
                    }

                    for (int indiceIzquierdo = 0; indiceIzquierdo < n; indiceIzquierdo++) {
                        temporal = 0;
                        for (int indiceDerecho = 0; indiceDerecho < m; indiceDerecho++) {
                            temporal = sumar(temporal, multiplicar(matrizVandermonde[indiceIzquierdo][indiceDerecho], nextShort[indiceDerecho]));
                        }
                        arrayListInfoDispersos.get(indiceIzquierdo)[indiceArreglos] = temporal;
                    }//fin del for interior
                    indiceArreglos++;
                }//fin del for exterior

                //############################################
                //Se guarda la info de cada arreglo en sus respectivos archivos de dispersos
                for (int indice = 0; indice < arrayListInfoDispersos.size(); indice++) {
                    int[] enterosParaDisperso = arrayListInfoDispersos.get(indice);
                    short[] arregloShorts = new short[enterosParaDisperso.length];
                    for (int indiceArray = 0; indiceArray < enterosParaDisperso.length; indiceArray++) {
                        arregloShorts[indiceArray] = (short) enterosParaDisperso[indiceArray];
                    }
                    ShortBuffer shortBufferFOS = ShortBuffer.wrap(arregloShorts);
                    ByteBuffer byteBufferFOS = ByteBuffer.allocate(arregloShorts.length * 2);

                    for (int indiceBuffer = 0; indiceBuffer < shortBufferFOS.capacity(); indiceBuffer++) {
                        byteBufferFOS.putShort(shortBufferFOS.get(indiceBuffer));
                    }
                    byteBufferFOS.flip();
                    seekBChannels[indice].write(byteBufferFOS);
                }

                byteBuffer.clear();
                //actualizarProgreso();
            }//fin del while

            //El siguiente for cierra los recursos utilizados para los dispersos.
            for (int i = 0; i < n; i++) {
                seekBChannels[i].close();
            }

            LocalDateTime ldtFinal = LocalDateTime.now();
            Timestamp tsFinal = Timestamp.valueOf(ldtFinal);

            /*logger.trace("Iniciamos en: " + ts);
            logger.trace("finalizamos en: " + tsFinal.toString());*/
            System.out.println("Iniciamos en: " + ts);
            System.out.println("Finalizamos en: " + tsFinal);
        } catch (FileNotFoundException fnfe) {
            /*logger.trace("Hubo una FileNotFoundException");
            logger.catching(fnfe);*/
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            /*logger.trace("Hubo una IOException");
            logger.catching(ioe);*/
            ioe.printStackTrace();
        } catch (Exception e) {
            /*logger.trace("Hubo una Exception");
            logger.catching(e);*/
            e.printStackTrace();
        }
    }

    /**
     * En este método se recibe la ruta donde se encuentra el archivo,
     * separador1=='\\' y separador2== '.'
     *
     * @param direccionArchivo ruta del archivo.
     * @param separador1 el separador de rutas asignado por el sistema
     * operativo.
     * @param separador2 es el punto antes de la extensión.
     * @return
     */
    /*public String separarNombreArchivo(String direccionArchivo, String separador1, String separador2) {

        String nombreArchivoNuevo = null;

        StringTokenizer st = new StringTokenizer(direccionArchivo);
        String nombreArchivo = null;

        while (st.hasMoreTokens()) {
            nombreArchivo = st.nextToken(separador1);
        }

        StringTokenizer st1 = new StringTokenizer(nombreArchivo);

        String arregloNombreArchivo[] = new String[2];
        int posicion = nombreArchivo.lastIndexOf(separador2);

        //si el archivo tiene extension
        if (posicion != -1) {

            arregloNombreArchivo[0] = nombreArchivo.substring(0, posicion);//nombre del archivo
            arregloNombreArchivo[1] = nombreArchivo.substring(posicion + 1);//extension del archivo

            nombreArchivoNuevo = arregloNombreArchivo[0] + "-" + arregloNombreArchivo[1];
            //nombreArchivoNuevo = arregloNombreArchivo[0] + "*" + arregloNombreArchivo[1];
        } else {  // si el archivo no tiene extension

            nombreArchivoNuevo = nombreArchivo;
        }
        System.out.println("El nombreArchivoNuevo en el dispersor vale:"+nombreArchivoNuevo);

        return nombreArchivoNuevo;
    }*/
    
    /**Este método sólo separa el nombre del archivo de la rut
     * @param direccionArchivo
     * @param separador1 
     * @return  
     */
    
    public String separarNombreArchivo(String direccionArchivo, String separador1) {

        StringTokenizer st = new StringTokenizer(direccionArchivo);
        String nombreArchivo = null;

        while (st.hasMoreTokens()) {
            nombreArchivo = st.nextToken(separador1);
        }
        System.out.println("El nombreArchivo en el dispersor vale:"+nombreArchivo);
        
        
        return nombreArchivo;
    }

    /**
     * *
     * Este metodo separa la ruta utilizando el parametro separador=='\\'
     *
     * @param ruta ruta del archivo que se va a separar.
     * @param separador el separador mediante el cuál se partirá la ruta.
     * @return
     */
    public String separarRuta(String ruta, String separador) {

        String rutaArchivo = null;

        int posicion = ruta.lastIndexOf(separador);
        System.out.println("posicion vale: "+posicion);
        
        rutaArchivo = ruta.substring(0, posicion);

        return rutaArchivo;
    }

    /**
     * Este método permite obtener únicamente el nombre del Archivo de la ruta
     * asignada.
     *
     * @param ruta ruta hacia el archivo que se va a procesar.
     * @param separador variable que muestra el separador asignado por el
     * sistema operativo.
     *
     * @return String nombreArchivo variable que contiene solo el nombre del
     * archivo.
     */
    public String obtenerNombreArchivo(String ruta, String separador) {
        String nombreArchivo = null;

        StringTokenizer st = new StringTokenizer(ruta);

        while (st.hasMoreTokens()) {
            nombreArchivo = st.nextToken(separador);
        }
        return nombreArchivo;
    }

    /**
     * Este método se utiliza para asignar el porcentaje que le corresponde a
     * cada uno de los items que vamos a respaldar en Babel.
     */
    public void asignarPorcentajeIndividual() {
        porcentajeIndividual = 100.0 / tamanioArchivo;
    }

    /**
     * Este método es para actualizar el progreso de la tarea actual y que en la
     * interfaz gráfica el usuario pueda ver el avance de la tarea.
     */
    public void actualizarProgreso() {
        porcentajeAcumulado = porcentajeAcumulado + porcentajeIndividual;
        System.out.println("Vamos al " + porcentajeAcumulado + " %");
        //updateProgress((double) (porcentajeAcumulado / 100.0), 1.0);
    }

}
