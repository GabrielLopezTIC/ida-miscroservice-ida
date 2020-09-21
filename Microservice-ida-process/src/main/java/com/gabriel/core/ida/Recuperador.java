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
 * Esta clase contiene los métodos necesarios para llevar a cabo la recuperación
 * de un archivo a partir de un umbral en el número de sus dispersos.
 *
 * @author Jorge
 */
//cambios añadidos para integrar con spring properties: atributo  rutaLogs añadido y agregado a contructor
public class Recuperador { //implements Runnable {

    private String logsDir;
    
    // private static Logger logger = LogManager.getLogger(Recuperador.class.getName());
    private int ordenCampo;
    private int campo, residuo, m, n, par;

    private int[] antiLogaritmo; //= new ExpLog().exp;
    private int[] logaritmo; //= new ExpLog().log;

    private int matrizVandermonde[][];

    private int primitivo;

    private int temporal = 0;
    private String[] nombreDispersos;
    private String rutaOrigen;
    private String rutaDestino;
    private String nombreArchivo;
    private double porcentajeAcumulado, porcentajeIndividual;
    private long tamanioArchivo, tamArchTemp;//Para ir sumando los chunks
    private int tamanioBuffer;

    public Recuperador(String logsDir,String[] nombreDispersos, String rutaOrigen, String rutaDestino, int tamanioBuffer, int ordenCampo, int polinomioPrimitivo, String unidad) {
       
	this.logsDir = logsDir;
	System.out.println("Entrando a recuperador");
	this.nombreDispersos = nombreDispersos;
        this.rutaOrigen = rutaOrigen;
        this.rutaDestino = rutaDestino;
        this.ordenCampo = ordenCampo;
        this.primitivo = polinomioPrimitivo;
        this.campo = (int) Math.pow(2, ordenCampo) - 1;
        obtenerResiduoMyN();

        if (unidad.equals("MB")) {
            this.tamanioBuffer = (tamanioBuffer * 1024 * 1024) * m * 2;//en MB, se multiplica por m para que sea múltiplo de los dispersos y * 2 para la conversion de shorts.
            System.out.println("el buffer en MB vale: " + this.tamanioBuffer);
        }

        if (unidad.equals("KB")) {
            this.tamanioBuffer = (tamanioBuffer * 1024) * m * 2;//en KB, se multiplica por m para que sea múltiplo de los dispersos y * 2 para la conversion de shorts.
            System.out.println("el buffer en KB vale: " + this.tamanioBuffer);
        }

    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void invertirMatrizGaussJordan() {
        int aux[] = new int[m * 2];
        int i = 0, j = 0, renglon = 0, columna = 0, pivote_renglon;

        for (j = 0; j < m; j++) {
            if (matrizVandermonde[renglon][columna] != 1) {//Si no es pivote
                dividirRenglonG(renglon, matrizVandermonde[renglon][columna]);
            }
            pivote_renglon = renglon; //Marcar como pivote
            for (i = 0; i < m; i++) {
                if (i != j) { //Si no es la diagonal
                    if (matrizVandermonde[i][j] != 0) { //Si no es un cero 
                        multiplicarConConstanteRenglonG(pivote_renglon, matrizVandermonde[i][j], aux);
                        sumarRestarRenglonG(i, aux);
                    }
                }
            }
            renglon++;
            columna++;
        }

    }

    /**
     * *******************************************************
     * OPERACIONES PARA INVERTIR MATRIZ POR GAUSS JORDAN
     * ********************************************************
     */
    public void sumarRestarRenglonG(int renglon, int aux[]) {
        int i;

        for (i = 0; i < m * 2; i++) {
            matrizVandermonde[renglon][i] = sumar(matrizVandermonde[renglon][i], aux[i]);
        }
    }

    public void multiplicarConConstanteRenglonG(int renglon, int constante, int aux[]) {
        int j;

        for (j = 0; j < m * 2; j++) {
            aux[j] = multiplicar(matrizVandermonde[renglon][j], constante);
        }
    }

    public void dividirRenglonG(int renglon, int div) {
        int i;

        for (i = 0; i < m * 2; i++) {
            matrizVandermonde[renglon][i] = dividir(matrizVandermonde[renglon][i], div);
        }
    }

    /**
     * **************************************************************
     * OPERACIONES CON CAMPOS
     * **************************************************************
     */
    public int sumar(int a, int b) {
        return (a ^ b);
    }

    public int multiplicar(int a, int b) {
        if ((a != 0) && (b != 0)) {
            return logaritmo[((antiLogaritmo[a] + antiLogaritmo[b]) % (campo))];
        } else {
            return 0;
        }
    }

    public int dividir(int a, int b) {
        if (b == 0) {
            System.out.println("El valor de b es 0 y se devuelve un 1234");
            return 1234;
        } else if (a == 0) {
            return 0;
        } else {
            return (logaritmo[((antiLogaritmo[a] + campo - antiLogaritmo[b]) % campo)]);
        }
    }

    /**
     * *
     * Este método permite recrear el nombre del archivo original a partir del
     * nombre de uno de los dispersos.
     *
     * @param nombreArchivo es el nombre de un disperso
     * @param separador1 separador que debe valer ='.'
     * @return
     */
    public String unirNombreArchivo(String nombreArchivo, String separador1) {

        StringTokenizer st = new StringTokenizer(nombreArchivo);

        int posicion = nombreArchivo.lastIndexOf(separador1);
        nombreArchivo = nombreArchivo.substring(0, posicion);
        
        System.out.println("nombreArchivo en el racuperador es: "+nombreArchivo);

        return nombreArchivo;
    }

    /**
     * *
     * Método para llevar a cabo los pasos necesarios para recuperar un archivo
     * a partir de m de sus dispersos.
     *
     * @param nombreDispersos arreglo con el nombre de m dispersos.
     * @param rutaOrigen ruta en la que se encuentran los dispersos.
     * @param rutaDestino ruta en la que se recuperará el archivo original.
     * @return un true (exito) o false (fracaso).
     */
//public boolean recuperarOriginal(String[] nombreDispersos, String rutaOrigen, String rutaDestino) {
    public boolean recuperarOriginal() throws IOException {
        boolean afirmacion = true;
        boolean bytesLeidosMenorTamanioBuffer = false;
        matrizVandermonde = new int[m][m * 2];
        GRIDA grida = new GRIDA(logsDir,16);
        grida.generarLogYAntiLog();
        logaritmo = grida.getLogaritmo();
        antiLogaritmo = grida.getAntiLogaritmo();
        nombreArchivo = unirNombreArchivo(nombreDispersos[0], ".");
        //nombreArchivo = unirNombreArchivo(nombreDispersos[0], ".", "*");

        int tamanioShortBuffer = 0;

//A partir de este punto dejamos todo como estaba solo sacamos los valores que deben ser para nuestros dispersos
        try { //En este bloque sacamos la matriz que traen consigo cada uno de los dispersos

            if (m == nombreDispersos.length) {
                boolean primerChunk = true;

                Path[] arregloPaths = new Path[m];
                for (int i = 0; i < m; i++) {
                    arregloPaths[i] = Paths.get(rutaOrigen, nombreDispersos[i]);
                }

                File fichero = new File(arregloPaths[0].toString());
                tamanioArchivo = fichero.length(); //Obtengo el tamanio de un disperso

                if (residuo == 0) {
                    tamanioArchivo = (((long) Math.ceil(tamanioArchivo / 2.0) - (4 + m)) * m);
                } else {
                    tamanioArchivo = (((long) Math.ceil(tamanioArchivo / 2.0) - (4 + m)) * m) - (m - residuo);
                }

                //logger.trace("tamanio del disperso: " + tamanioArchivo);
                SeekableByteChannel[] arregloSBChannel = new SeekableByteChannel[m];

                for (int i = 0; i < m; i++) {
                    arregloSBChannel[i] = Files.newByteChannel(arregloPaths[i], EnumSet.of(StandardOpenOption.READ));
                }

                ByteBuffer[] arregloBBuffer = new ByteBuffer[m];
                for (int i = 0; i < m; i++) {
                    if (primerChunk) {
                        arregloBBuffer[i] = ByteBuffer.allocate(tamanioBuffer);
                        arregloBBuffer[i].clear();
                    } else {

                    }
                }

                //A partir de aquí se deberá leer por chunks cada disperso
                LocalDateTime ldt = LocalDateTime.now();
                Timestamp tiempoInicial = Timestamp.valueOf(ldt);

                int bytesLeidos = 0;
                File carpeta = new File(rutaDestino);
                carpeta.mkdirs();
                //FileOutputStream fos = new FileOutputStream(rutaDestino + nombreArchivo);
                //El siguiente channel es para construir el archivo original
                SeekableByteChannel archOriginalSeekBC = null;
                try {
                    archOriginalSeekBC = Files.newByteChannel(Paths.get(rutaDestino, nombreArchivo), EnumSet.of(StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE));
                } catch (NoSuchFileException nsfe) {
                    archOriginalSeekBC = Files.newByteChannel(Paths.get(rutaDestino, nombreArchivo), EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE));
                }

                while ((bytesLeidos = arregloSBChannel[0].read(arregloBBuffer[0])) > 0) {
                    System.out.println("Se leyeron realmente " + bytesLeidos + " bytes ");

                    for (int i = 1; i < m; i++) {
                        arregloSBChannel[i].read(arregloBBuffer[i]);
                    }

                    for (int i = 0; i < m; i++) {
                        arregloBBuffer[i].flip();
                    }

                    ShortBuffer[] arregloShortBuffer = new ShortBuffer[m];

                    if (bytesLeidos < tamanioBuffer) {
                        bytesLeidosMenorTamanioBuffer = true;
                    }
                    //Como se están trabajando con shorts, vamos a tener un número par de bytes
                    //así que el siguiente bloque de código se va a ejecutar aún cuando 
                    //el número de bytes leídos sean menores al tamanioBuffer
                    for (int i = 0; i < arregloShortBuffer.length; i++) {
                        arregloShortBuffer[i] = arregloBBuffer[i].asShortBuffer();
                    }
                    //##################################################################
                    tamanioShortBuffer = arregloShortBuffer[0].capacity();
                    int tamanio = 0;
                    int tamanioArreglo = 0;

                    if (primerChunk) { //Este if se utiliza para obtener la matriz de vandermonde

                        for (int indiceIzquierdo = 0; indiceIzquierdo < m; indiceIzquierdo++) {
                            ShortBuffer shortBuffer = arregloShortBuffer[indiceIzquierdo];

                            for (int indiceDerecho = 0; indiceDerecho < m; indiceDerecho++) {
                                short valorMatriz = shortBuffer.get(indiceDerecho + 4);//el +4 es por que ya se obtuvieron r,m, n y par
                                if (valorMatriz < 0) {
                                    matrizVandermonde[indiceIzquierdo][indiceDerecho] = valorMatriz & 0xffff;
                                } else {
                                    matrizVandermonde[indiceIzquierdo][indiceDerecho] = valorMatriz;
                                }
                            }
                        }//fin del for
                        agregarMatrizIdentidad();
                        invertirMatrizGaussJordan();//Sólo lo debemos hacer una sola vez
                        tamanio = 4 + m; // r,m y n son 3 + las m posiciones del renglón de la matriz de vandermonde guardada en los dispersos.
                        //tamArchTemp = tamanio;

                        tamanioArreglo = ((int) Math.ceil(bytesLeidos / 2.0) - (4 + m)) * m;

                        if (residuo > 0 && bytesLeidosMenorTamanioBuffer) {
                            tamanioArreglo = (((int) Math.ceil(bytesLeidos / 2.0) - (4 + m)) * m) - (m - residuo);
                        }

                    } // fin del if(primerChunk)
                    /* logger.trace("Ya salimos del ajuste para el primer chunk");
                    logger.trace("El tamanio de arregloRecuperado es: " + arregloArchivoRecuperado.length);
                    logger.trace("El residuo en la recuperación vale: "+residuo);*/

                    if (!primerChunk) {// residuo==0, bytesLeidosMenorTamanioBuffer, !bytesLeidosMenorTamanioBuffer
                        tamanioArreglo = (int) Math.ceil(bytesLeidos / 2.0) * m;
                    }

                    if (!primerChunk && residuo > 0 && bytesLeidosMenorTamanioBuffer) {
                        tamanioArreglo = (int) Math.ceil(bytesLeidos / 2.0) * m - (m - residuo);
                    }

                    int arregloArchivoRecuperado[] = new int[tamanioArreglo];

                    /**
                     * Aqui falta cotejar que pasa cuando los bytes leidos son
                     * menor al tamanioBuffer Cambiar el ArrayList por un array
                     * de Primitivos
                     */
                    primerChunk = false;
                    int datosArchivos[] = new int[m];
                    int datosRecuperados[] = new int[m];
                    short shortLeido = 0;
                    int indiceArregloArchRec = 0;

                    //tamanio=4+m ya que son las posiciones que ocupan el residuo,n, m y par, junto con los tres valores de la matriz.
                    for (; tamanio < tamanioShortBuffer; tamanio++) {
                        for (int i = 0; i < m; i++) { // este for permite recorrer los m buffers
                            ShortBuffer shortBuffer = arregloShortBuffer[i];

                            shortLeido = shortBuffer.get(tamanio);
                            if (shortLeido < 0) {
                                int enteroPositivo = shortLeido & (0xffff); //lo convertimos en positivo
                                datosArchivos[i] = enteroPositivo;
                            } else {
                                datosArchivos[i] = shortLeido;
                            }
                        }

                        for (int indiceIzquierdo = 0, indiceRecuperados = 0; indiceIzquierdo < m; indiceIzquierdo++, indiceRecuperados++) {
                            temporal = 0;
                            for (int indiceDerecho = m, indiceDatos = 0; indiceDerecho < 2 * m; indiceDerecho++, indiceDatos++) {
                                temporal = sumar(temporal, multiplicar(matrizVandermonde[indiceIzquierdo][indiceDerecho], datosArchivos[indiceDatos]));
                            }
                            datosRecuperados[indiceRecuperados] = temporal;
                        }

                        for (int i = 0; i < datosRecuperados.length; i++) {
                            if (bytesLeidosMenorTamanioBuffer) {
                                if (tamArchTemp < tamanioArchivo) {//Para quitar ceros en caso de que haya rellenos
                                    arregloArchivoRecuperado[indiceArregloArchRec] = datosRecuperados[i];
                                }
                                tamArchTemp++;
                                indiceArregloArchRec++;
                            } else {
                                arregloArchivoRecuperado[indiceArregloArchRec] = datosRecuperados[i];
                                tamArchTemp++;
                                indiceArregloArchRec++;
                            }
                        }

                    }

                    ByteBuffer byteBuffer = ByteBuffer.allocate(arregloArchivoRecuperado.length * 2); // par == 0 y !bytesLeidosMenorTamanioBuffer
                    if (par == 1 && bytesLeidosMenorTamanioBuffer) {
                        byteBuffer = ByteBuffer.allocate((arregloArchivoRecuperado.length * 2) - 1);
                    }

                    for (int indice = 0; indice < arregloArchivoRecuperado.length; indice++) {
                        if (par == 1 && indice == arregloArchivoRecuperado.length - 1 && bytesLeidosMenorTamanioBuffer) {
                            short valor = (short) arregloArchivoRecuperado[indice];
                            byte valorB = (byte) valor;
                            byteBuffer.put(valorB);
                        } else {
                            short valor = (short) arregloArchivoRecuperado[indice];
                            byteBuffer.putShort(valor);
                        }
                    }

                    byteBuffer.flip();

                    archOriginalSeekBC.write(byteBuffer);

                }//fin del while que permite continuar leyendo bytes de los dispersos //fin del if de la linea 169
                LocalDateTime ldtFinal = LocalDateTime.now();
                Timestamp tiempoFinal = Timestamp.valueOf(ldtFinal);
                System.out.println("tiempoInicial vale: " + tiempoInicial);
                System.out.println("tiempoFinal vale: " + tiempoFinal);
                archOriginalSeekBC.close();
                //El siguiente for es para cerrar los channels 
                for (int i = 0; i < m; i++) {
                    arregloSBChannel[i].close();
                }
                System.out.println("Se terminó la recuperación");
            } else {
                System.out.println("Introduzca el nombre de " + m + " dispersos para la recuperacion");
            }
        } catch (IOException ioe) {
            /*logger.trace("Hubo una IOException");
            logger.catching(ioe);*/
            ioe.printStackTrace();
            afirmacion = false;
        } catch (Exception e) {
            /*logger.trace("Hubo una Exception");
            logger.catching(e);*/
            e.printStackTrace();
            afirmacion = false;
        } finally {
            return afirmacion;
        }
    }

    /**
     * Este método se utiliza para asignar el porcentaje que le corresponde a
     * cada uno de los items que vamos a respaldar en Babel.
     */
    public void asignarPorcentajeIndividual() {
        porcentajeIndividual = 100.0 / (tamanioArchivo * 3);//Se multiplica X3 ya que son 3 dispersos
    }

    /*@Override
    public void run() {
        recuperarOriginal(nombreDispersos, rutaOrigen, rutaDestino, tamanioBuffer);
    }*/
    /**
     * Este método se utiliza para obtener el residuo la m y n desde un
     * disperso. Estos datos son necesarios para ajustar las operaciones de
     * recuperación.
     *
     */
    private void obtenerResiduoMyN() {
        try {
            System.out.println("rutaOrigen: " + rutaOrigen);
            System.out.println("nombreDispersos[0] vale: " + nombreDispersos[0]);
            SeekableByteChannel seekBCh = Files.newByteChannel(Paths.get(rutaOrigen, nombreDispersos[0]), EnumSet.of(StandardOpenOption.READ));
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            //FileInputStream fis = new FileInputStream(rutaDispersoCompleta);
            seekBCh.read(byteBuffer);
            byteBuffer.flip();
            residuo = byteBuffer.getShort();
            System.out.println("El residuo vale: " + residuo);
            //residuo = fis.read(); //Obtenemos el residuo;
            //n = fis.read();
            n = byteBuffer.getShort();
            System.out.println("El n vale: " + n);
            //m = fis.read();
            m = byteBuffer.getShort();
            System.out.println("El m vale: " + m);

            par = byteBuffer.getShort();
            System.out.println("Variable par vale: " + par);

            seekBCh.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Este método agrega la matriz identidad a la matriz de vandermonde para
     * poder realizar la inversa con el método de Gauss-Jordan
     */
    private void agregarMatrizIdentidad() {
        for (int i = 0; i < m; i++) {
            for (int j = m; j < m * 2; j++) {
                if (i + m == j) {
                    matrizVandermonde[i][j] = 1;
                } else {
                    matrizVandermonde[i][j] = 0;
                }
            }
        }
    }

}
