package org.NoTokensLeft;

import org.NoTokensLeft.dominio.ResultadoRuta;
import org.NoTokensLeft.servicios.ServicioMapa;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class Main {
    public static void main(String[] args) {
        if (args.length > 1) {
            System.out.println("Uso: java -jar RetoAvanzadoOlimpiadasJava-1.0-SNAPSHOT.jar [archivo-del-mapa]");
            return;
        }
        try (Scanner entrada = new Scanner(System.in, StandardCharsets.UTF_8)) {
            Path archivo = args.length == 0 ? Path.of("datos", "mapa.txt") : Path.of(args[0]);
            ServicioMapa servicio = new ServicioMapa(archivo);
            System.out.println("Mapa de sedes. Archivo local: " + archivo.toAbsolutePath());
            var consulta = servicio.ultimaConsulta();
            if (consulta != null) {
                System.out.println("Última consulta recuperada: " + consulta.origen() + " -> " + consulta.destino());
                mostrarResultado(consulta.resultado());
            }
            ejecutarMenu(entrada, servicio);
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada.");
        } catch (InvalidPathException e) {
            System.out.println("Error: la ruta del archivo no es válida.");
        } catch (IOException | SecurityException e) {
            System.out.println("No se pudo abrir el mapa: " + e.getMessage());
            System.out.println("Revise el archivo y sus permisos antes de volver a iniciar.");
        }
    }

    private static void ejecutarMenu(Scanner entrada, ServicioMapa servicio) {
        while (true) {
            System.out.println("""

                    1. Agregar sede
                    2. Agregar o actualizar conexión dirigida
                    3. Calcular ruta mínima
                    4. Mostrar mapa
                    5. Importar mapa (reemplaza el actual)
                    6. Exportar mapa
                    0. Salir
                    """);
            System.out.print("Opción: ");
            if (!entrada.hasNextLine()) {
                return;
            }
            String opcion = entrada.nextLine().strip();
            try {
                switch (opcion) {
                    case "1" -> {
                        servicio.agregarSede(leer(entrada, "Nombre de la sede: "));
                        System.out.println("Sede guardada.");
                    }
                    case "2" -> {
                        String origen = leer(entrada, "Origen: ");
                        String destino = leer(entrada, "Destino: ");
                        int costo = Integer.parseInt(leer(entrada, "Costo entero no negativo: ").strip());
                        servicio.conectar(origen, destino, costo);
                        System.out.println("Conexión guardada.");
                    }
                    case "3" -> {
                        ResultadoRuta resultado = servicio.calcularRuta(
                                leer(entrada, "Origen: "), leer(entrada, "Destino: "));
                        mostrarResultado(resultado);
                    }
                    case "4" -> mostrarMapa(servicio);
                    case "5" -> {
                        servicio.importar(Path.of(leer(entrada, "Archivo a importar (reemplaza el mapa actual): ")));
                        System.out.println("Mapa importado y guardado.");
                    }
                    case "6" -> {
                        servicio.exportar(Path.of(leer(entrada, "Archivo de destino (se reemplaza si existe): ")));
                        System.out.println("Mapa exportado.");
                    }
                    case "0" -> {
                        System.out.println("Hasta luego. Los cambios ya están guardados.");
                        return;
                    }
                    default -> System.out.println("Opción inválida. Seleccione un número del menú.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: el costo debe ser un entero entre 0 y 2147483647.");
            } catch (InvalidPathException e) {
                System.out.println("Error: la ruta del archivo no es válida.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (IOException | SecurityException e) {
                System.out.println("No se pudo completar la operación: " + e.getMessage());
            }
        }
    }

    private static String leer(Scanner entrada, String mensaje) {
        System.out.print(mensaje);
        return entrada.nextLine();
    }

    private static void mostrarResultado(ResultadoRuta resultado) {
        if (resultado.estado() == ResultadoRuta.Estado.SIN_RUTA) {
            System.out.println("SIN_RUTA");
        } else {
            System.out.println("Ruta: " + String.join(" -> ", resultado.ruta()));
            System.out.println("Costo total: " + resultado.costoTotal());
        }
    }

    private static void mostrarMapa(ServicioMapa servicio) {
        if (servicio.sedes().isEmpty()) {
            System.out.println("El mapa no tiene sedes.");
            return;
        }
        for (String sede : servicio.sedes()) {
            System.out.println(sede);
            var conexiones = servicio.conexionesDesde(sede);
            if (conexiones.isEmpty()) {
                System.out.println("  Sin conexiones de salida.");
            }
            conexiones.forEach((destino, costo) ->
                    System.out.println("  -> " + destino + " (costo " + costo + ")"));
        }
    }
}
