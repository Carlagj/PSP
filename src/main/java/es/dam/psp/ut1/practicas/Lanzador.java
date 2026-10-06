package es.dam.psp.ut1.practicas;

import es.dam.psp.ut1.Jvm;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;
// Necesitarás además: java.io.File, java.nio.file.Files, java.nio.file.StandardOpenOption
// y java.util.concurrent.TimeUnit

/*
 * Lanzador.java · Práctica 3 · Lanzador de órdenes. Completa los
 *
 * Apuntes UT-1, apartados 5.1 (ProcessBuilder, Jvm.comandoShell), 5.3 (leer la salida),
 * 6.1 (waitFor con límite, destroy) y 6.2 (no bloquearse leyendo).
 * Cómo ejecutarlo: triángulo ▶ junto a main, sin argumentos; escribe las órdenes en la consola
 * de IntelliJ (p. ej. "echo hola", "ls noexiste", "sleep 10" o, en Windows, "ping -n 11 127.0.0.1").
 *
 * Bucle: pide una orden al usuario, la ejecuta como proceso hijo y muestra
 *   - su salida (stdout y stderr juntas), con cada línea numerada (3.a),
 *   - su código de salida y el tiempo que ha tardado en milisegundos (3.b).
 * Si la orden tarda más de TIMEOUT_S segundos, se termina el proceso y se avisa (3.c).
 * Cada ejecución se añade a historial.txt con el formato:  orden;codigo;milisegundos (3.d)
 * La orden "salir" termina el programa.
 */
public class Lanzador {

    static final long TIMEOUT_S = 5;   // segundos que se deja trabajar a cada orden

    public static void main(String[] args) throws IOException, InterruptedException {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Lanzador PSP · sistema: " + System.getProperty("os.name") + " · escribe 'salir' para terminar");
        while (true) {
            System.out.print("> ");
            String orden = teclado.readLine();          // null = fin de la entrada (Ctrl+D)
            if (orden == null) break;
            orden = orden.trim();
            if (orden.equals("salir")) break;
            if (orden.isEmpty()) continue;


            long tiempoInicio = System.currentTimeMillis();
            int codigoSalida = 0;


            File ficheroTemporal = File.createTempFile("lanzador_", ".tmp");


            List<String> comando = Jvm.comandoShell(orden);
            ProcessBuilder pb = new ProcessBuilder(comando);


            pb.redirectOutput(ficheroTemporal);
            pb.redirectErrorStream(true);

            Process proceso = pb.start();

            boolean terminado = proceso.waitFor(TIMEOUT_S, TimeUnit.SECONDS);
            if (!terminado) {
                proceso.destroyForcibly();
                codigoSalida = -1;
            } else {
                codigoSalida = proceso.exitValue();
            }

            BufferedReader lector = new BufferedReader(new FileReader(ficheroTemporal));
            String linea;
            int numLinea = 1;
            while ((linea = lector.readLine()) != null) {
                System.out.println(numLinea + ": " + linea);
                numLinea++;
            }
            lector.close();
            ficheroTemporal.delete();

            long tiempoFin = System.currentTimeMillis();
            long milisegundos = tiempoFin - tiempoInicio;


            System.out.println("[código " + codigoSalida + " " + milisegundos + " ms]");


            PrintWriter escritor = new PrintWriter(new FileWriter("historial.txt", true));
            escritor.println(orden + "; " + codigoSalida + "; " + milisegundos);
            escritor.close();
        }



        }
    }

