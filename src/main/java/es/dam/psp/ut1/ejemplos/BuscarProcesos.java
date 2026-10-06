package es.dam.psp.ut1.ejemplos;

import java.time.Instant;

/**
 * E6 · Un "ps" casero con ProcessHandle: información de los procesos del sistema.
 *
 * Apuntes UT-1, apartado 6.4. Base de la Práctica 2.c (practicas/BuscarProcesos.java).
 * Cómo ejecutarlo: triángulo ▶ junto a main, sin argumentos.
 * Muestra el PID propio y el del padre, y una tabla PID, PPID, USUARIO y COMANDO con los
 * 15 primeros procesos visibles. Solo verás los datos que el sistema operativo permite leer
 * a tu usuario: cada dato de info() es un Optional que puede estar vacío.
 */
public class BuscarProcesos {

    public static void main(String[] args) {

        if (args.length == 0){
            System.out.println("Introduce una cadena de busqueda como argumento. ");
            return;
        }

        String filtro = args[0].toLowerCase();

        ProcessHandle yo = ProcessHandle.current();
        System.out.println("Soy el PID " + yo.pid() + ", mi padre es "
                + yo.parent().map(ProcessHandle::pid).orElse(-1L));
        System.out.printf("%-8s %-8s %-12s %s%n", "PID", "PPID", "USUARIO", "COMANDO");

        ProcessHandle.allProcesses()
                .filter(p -> p.info().command().isPresent())   // solo los procesos cuyo comando podemos leer

                .filter(p -> p.info().command().get().toLowerCase().contains(filtro))
                .forEach(p -> {
                    ProcessHandle.Info info = p.info();

                    long pid = p.pid();
                    String ppid = p.parent().map(padre -> String.valueOf(padre.pid())).orElse("-");
                    String usuario = ultimos(info.user().orElse("?"),12);

                    String inicio = info.startInstant()
                            .map(Instant::toString)
                            .map(s -> s.length() > 22 ? s.substring(0, 22) : s)
                            .orElse("N/A");

                    String cpu = info.totalCpuDuration()
                            .map(d -> d.toMillis() + " ms")
                            .orElse("N/A");

                    String comando = ultimos(info.command().get(), 40);

                    System.out.printf("%-8d %-8s %-12s %s%n",
                           pid,ppid,usuario,inicio,cpu,comando);
                });
    }

    /** Los n últimos caracteres de s (o s entera si es más corta). Equivale a takeLast(n) de Kotlin. */
    static String ultimos(String s, int n) {
        return s.length() <= n ? s : s.substring(s.length() - n);
    }
}
