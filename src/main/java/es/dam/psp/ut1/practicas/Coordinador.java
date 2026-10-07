package es.dam.psp.ut1.practicas;

import es.dam.psp.ut1.Jvm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/*
 * Coordinador.java · Práctica 4 · proceso PADRE. Completa los TODO 4.a … 4.e (letras de la práctica).
 *
 * Apuntes UT-1, apartados 5.2 (Jvm.proceso), 5.1 (redirectInput), 6 (leer antes de esperar)
 * y 6.3 (lanzar todos y después esperar a todos).
 * Antes: ejecuta practicas/GeneradorDatos.java para crear datos/ en la raíz del proyecto.
 * Cómo ejecutarlo: triángulo ▶ junto a main, sin argumentos. Cada hijo es practicas/Contador.java.
 *
 * Reparte los ficheros de datos/ entre procesos hijo Contador, recoge sus resultados
 * y muestra el total. Compara el tiempo en modo secuencial y en modo concurrente.
 * Mejora opcional: añade concurrenteLimitado(ficheros, maximo) y mídelo con 1, 2, 4 y 8.
 */
public class Coordinador {

    /*
     * Resultado que devuelve un hijo para un fichero.
     * Un record (Java 16+) es como una data class de Kotlin: constructor, getters
     * (r.lineas(), r.palabras()...), equals, hashCode y toString automáticos.
     */
    record Resultado(String fichero, long lineas, long palabras, long caracteres, String masFrecuente) { }

    static final String CLASE_HIJO = "es.dam.psp.ut1.practicas.Contador";   // main de Contador.java

    /*
     * Crea y ARRANCA un proceso Contador cuya entrada estándar sea el fichero.
     * Pista: Jvm.proceso(...) y redirectInput(...). Hereda la salida de error del hijo.
     */
    static Process lanzarContador(File fichero) throws IOException {
       return Jvm.proceso(CLASE_HIJO)
               .redirectInput(fichero)
               .redirectError(ProcessBuilder.Redirect.INHERIT)
               .start();
    }

    /*
     * Lee la línea que ha escrito el hijo, espera a que termine y construye el Resultado.
     * Si el código de salida no es 0, lanza una IllegalStateException con un mensaje claro.
     */
    static Resultado recogerResultado(File fichero, Process hijo) throws IOException, InterruptedException {
        String linea;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(hijo.getInputStream(), StandardCharsets.UTF_8))){
            linea = reader.readLine();
        }
        int exitCode = hijo.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("El proceso hijo para " + fichero.getName() + " terminó con error (código " + exitCode + ")");
        }
        if (linea == null || linea.isBlank()) {
            throw new IllegalStateException("El hijo no devolvió ningún resultado para " + fichero.getName());
        }
        String[] partes = linea.split(";", 4);
        return new Resultado(
                fichero.getName(),
                Long.parseLong(partes[0].trim()),
                Long.parseLong(partes[1].trim()),
                Long.parseLong(partes[2].trim()),
                partes[3].trim()
        );
    }

    /* Procesa los ficheros uno detrás de otro: lanzar, esperar, lanzar, esperar... */
    static List<Resultado> secuencial(List<File> ficheros) throws IOException, InterruptedException {
        List<Resultado> resultados = new ArrayList<>();
        for (File fichero : ficheros) {
            Process hijo = lanzarContador(fichero);
            Resultado res = recogerResultado(fichero, hijo);
            resultados.add(res);
        }
        return resultados;
    }

    /* Lanza TODOS los hijos a la vez y después recoge los resultados. */
    static List<Resultado> concurrente(List<File> ficheros) throws IOException, InterruptedException {
        List<Process> hijos = new ArrayList<>();
        for (File fichero : ficheros) {
            hijos.add(lanzarContador(fichero));
        }

        List<Resultado> resultados = new ArrayList<>();
        for (int i = 0; i < ficheros.size(); i++) {
            resultados.add(recogerResultado(ficheros.get(i), hijos.get(i)));
        }
        return resultados;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // Ruta relativa: datos/ dentro del directorio de trabajo (la raíz del proyecto en IntelliJ)
        File[] lista = new File("datos").listFiles(f -> f.getName().endsWith(".txt"));
        if (lista == null || lista.length == 0) {
            System.out.println("No hay ficheros en datos/. Ejecuta antes GeneradorDatos.java");
            return;
        }
        List<File> ficheros = Arrays.stream(lista).sorted(Comparator.comparing(File::getName)).toList();
        System.out.println("Procesadores disponibles: " + Runtime.getRuntime().availableProcessors());

        long inicio = System.nanoTime();
        List<Resultado> r1 = secuencial(ficheros);
        long t1 = System.nanoTime() - inicio;

        inicio = System.nanoTime();
        List<Resultado> r2 = concurrente(ficheros);
        long t2 = System.nanoTime() - inicio;

        // TODO 4.e: muestra una tabla con el resultado de cada fichero y una fila TOTAL
        //  (líneas, palabras y caracteres sumados). Comprueba que r1 y r2 coinciden.

        // TODO 4.e: muestra el resultado de cada fichero y una fila TOTAL. Comprueba que r1 y r2 coinciden.
        long totalLineas = 0;
        long totalPalabras = 0;
        long totalCaracteres = 0;

        System.out.println("\n--- RESULTADOS ---");
        for (int i = 0; i < r1.size(); i++) {
            Resultado res1 = r1.get(i);
            System.out.println("Fichero: " + res1.fichero() + " | Líneas: " + res1.lineas() + " | Palabras: " + res1.palabras() + " | Caracteres: " + res1.caracteres() + " | Más frecuente: " + res1.masFrecuente());

            totalLineas += res1.lineas();
            totalPalabras += res1.palabras();
            totalCaracteres += res1.caracteres();
        }

        System.out.println("--------------------------------------------------");
        System.out.println("TOTAL -> Líneas: " + totalLineas + " | Palabras: " + totalPalabras + " | Caracteres: " + totalCaracteres);


        if (r1.equals(r2)) {
            System.out.println("\nComprobación: Los resultados secuenciales y concurrentes coinciden correctamente.");
        } else {
            System.out.println("\nComprobación: ¡Cuidado! Los resultados no coinciden.");
        }

        System.out.printf("Secuencial:  %.3f s%n", t1 / 1e9);
        System.out.printf("Concurrente: %.3f s%n", t2 / 1e9);
        // Práctica 4.f: ejecútalo tres veces y calcula la aceleración t1 / t2
    }
}
