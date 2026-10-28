package org.NoTokensLeft;

import org.NoTokensLeft.servicios.ServicioMapa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @TempDir
    Path temporal;

    @Test
    void consolaImportaConsultaYExportaElMapa() throws IOException {
        Path entrada = temporal.resolve("ejemplo.txt");
        Files.writeString(entrada, """
                MAPA_SEDES;1
                SEDE;A
                SEDE;B
                SEDE;C
                SEDE;Aislada
                CONEXION;A;B;4
                CONEXION;A;C;1
                CONEXION;C;B;2
                """);
        Path copia = temporal.resolve("copia.txt");

        String salida = ejecutar("5\n" + entrada + "\n3\nA\nB\n3\nA\nAislada\n3\nA\nA\n6\n" + copia + "\n0\n");

        assertTrue(salida.contains("Ruta: A -> C -> B"));
        assertTrue(salida.contains("Costo total: 3"));
        assertTrue(salida.contains("SIN_RUTA"));
        assertTrue(salida.contains("Costo total: 0"));
        String reinicio = ejecutar("0\n");
        assertTrue(reinicio.contains("Última consulta recuperada: A -> A"));
        assertTrue(reinicio.contains("Costo total: 0"));
        assertEquals(3L, new ServicioMapa(copia).calcularRuta("A", "B").costoTotal());
    }

    @Test
    void entradaInvalidaPermiteSeguirUsandoElMenu() throws IOException {
        String salida = ejecutar("1\nA\n1\nB\n2\nA\nB\n-1\n2\nA\nB\ntexto\n3\nA\nB\n0\n");

        assertTrue(salida.contains("El costo no puede ser negativo"));
        assertTrue(salida.contains("el costo debe ser un entero"));
        assertTrue(salida.contains("SIN_RUTA"));
        assertTrue(new ServicioMapa(temporal.resolve("mapa.txt")).conexionesDesde("A").isEmpty());
    }

    @Test
    void finDeEntradaDuranteUnaOperacionNoGuardaDatosParciales() {
        String salida = ejecutar("1\n");

        assertTrue(salida.contains("Entrada finalizada"));
        assertFalse(Files.exists(temporal.resolve("mapa.txt")));
    }

    @Test
    void archivoCorruptoSeReportaSinSobrescribirlo() throws IOException {
        Path archivo = temporal.resolve("mapa.txt");
        Files.writeString(archivo, "archivo inválido");

        String salida = ejecutar("0\n");

        assertTrue(salida.contains("No se pudo abrir el mapa"));
        assertEquals("archivo inválido", Files.readString(archivo));
    }

    private String ejecutar(String comandos) {
        var entradaOriginal = System.in;
        var salidaOriginal = System.out;
        ByteArrayOutputStream captura = new ByteArrayOutputStream();
        try (PrintStream salida = new PrintStream(captura, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream(comandos.getBytes(StandardCharsets.UTF_8)));
            System.setOut(salida);
            Main.main(new String[]{temporal.resolve("mapa.txt").toString()});
            return captura.toString(StandardCharsets.UTF_8);
        } finally {
            System.setIn(entradaOriginal);
            System.setOut(salidaOriginal);
        }
    }
}
