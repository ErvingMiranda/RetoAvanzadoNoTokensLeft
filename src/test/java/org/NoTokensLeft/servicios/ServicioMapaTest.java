package org.NoTokensLeft.servicios;

import org.NoTokensLeft.dominio.ResultadoRuta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ServicioMapaTest {
    @TempDir
    Path temporal;

    @Test
    void iniciaVacioCuandoNoExisteElArchivo() throws IOException {
        ServicioMapa servicio = new ServicioMapa(temporal.resolve("nuevo.txt"));

        assertTrue(servicio.sedes().isEmpty());
    }

    @Test
    void recuperaElEstadoYLaRutaAlReiniciar() throws IOException {
        Path archivo = temporal.resolve("datos/mapa.txt");
        ServicioMapa servicio = new ServicioMapa(archivo);
        for (String sede : List.of("A", "B", "C", "Aislada")) {
            servicio.agregarSede(sede);
        }
        servicio.conectar("A", "B", 4);
        servicio.conectar("A", "C", 1);
        servicio.conectar("C", "B", 2);
        ResultadoRuta original = servicio.calcularRuta("A", "B");

        ServicioMapa reiniciado = new ServicioMapa(archivo);
        assertEquals("A", reiniciado.ultimaConsulta().origen());
        assertEquals("B", reiniciado.ultimaConsulta().destino());
        assertEquals(original, reiniciado.ultimaConsulta().resultado());
        ResultadoRuta ruta = reiniciado.calcularRuta("A", "B");

        assertEquals(3L, ruta.costoTotal());
        assertEquals(List.of("A", "C", "B"), ruta.ruta());
        assertEquals(ResultadoRuta.Estado.SIN_RUTA, reiniciado.calcularRuta("A", "Aislada").estado());
    }

    @Test
    void importaReemplazandoElMapaYPersisteElReemplazo() throws IOException {
        Path archivo = temporal.resolve("mapa.txt");
        ServicioMapa servicio = new ServicioMapa(archivo);
        servicio.agregarSede("Anterior");
        Path entrada = temporal.resolve("importado.txt");
        Files.writeString(entrada, "MAPA_SEDES;1\nSEDE;A\nSEDE;B\nCONEXION;A;B;7\n");

        servicio.importar(entrada);

        assertEquals(Set.of("A", "B"), servicio.sedes());
        assertEquals(7L, new ServicioMapa(archivo).calcularRuta("A", "B").costoTotal());
    }

    @Test
    void importacionInvalidaConservaLaMemoriaYElArchivo() throws IOException {
        Path archivo = temporal.resolve("mapa.txt");
        ServicioMapa servicio = new ServicioMapa(archivo);
        servicio.agregarSede("Original");
        String anterior = Files.readString(archivo);
        Path entrada = temporal.resolve("invalido.txt");
        Files.writeString(entrada, "MAPA_SEDES;1\nSEDE;A\nSEDE;B\nCONEXION;A;B;-1\n");

        assertThrows(IOException.class, () -> servicio.importar(entrada));
        assertEquals(Set.of("Original"), servicio.sedes());
        assertEquals(anterior, Files.readString(archivo));
    }

    @Test
    void falloDeGuardadoNoAplicaElCambioEnMemoria() throws IOException {
        Path bloqueo = temporal.resolve("bloqueo");
        ServicioMapa servicio = new ServicioMapa(bloqueo.resolve("mapa.txt"));
        Files.writeString(bloqueo, "Este archivo impide crear la carpeta.");

        assertThrows(IOException.class, () -> servicio.agregarSede("A"));
        assertTrue(servicio.sedes().isEmpty());
        assertEquals("Este archivo impide crear la carpeta.", Files.readString(bloqueo));
    }

    @Test
    void pesoNegativoNoModificaElArchivoPersistido() throws IOException {
        Path archivo = temporal.resolve("mapa.txt");
        ServicioMapa servicio = new ServicioMapa(archivo);
        servicio.agregarSede("A");
        servicio.agregarSede("B");
        String anterior = Files.readString(archivo);

        assertThrows(IllegalArgumentException.class, () -> servicio.conectar("A", "B", -1));
        assertTrue(servicio.conexionesDesde("A").isEmpty());
        assertEquals(anterior, Files.readString(archivo));
    }

    @Test
    void noReiniciaNiSobrescribeUnArchivoCorrupto() throws IOException {
        Path archivo = temporal.resolve("mapa.txt");
        Files.writeString(archivo, "contenido dañado");

        assertThrows(IOException.class, () -> new ServicioMapa(archivo));
        assertEquals("contenido dañado", Files.readString(archivo));
    }

    @Test
    void exportacionSePuedeCargarEnOtraInstancia() throws IOException {
        ServicioMapa servicio = new ServicioMapa(temporal.resolve("original.txt"));
        servicio.agregarSede("A");
        Path exportacion = temporal.resolve("copia.txt");

        servicio.exportar(exportacion);

        assertEquals(List.of("A"), new ServicioMapa(exportacion).calcularRuta("A", "A").ruta());
    }

    @Test
    void recargaSinRutaYDescartaElResultadoAlCambiarElMapa() throws IOException {
        Path archivo = temporal.resolve("mapa.txt");
        ServicioMapa servicio = new ServicioMapa(archivo);
        servicio.agregarSede("A");
        servicio.agregarSede("B");
        servicio.calcularRuta("A", "B");

        ServicioMapa reiniciado = new ServicioMapa(archivo);
        assertEquals(ResultadoRuta.Estado.SIN_RUTA, reiniciado.ultimaConsulta().resultado().estado());

        reiniciado.conectar("A", "B", 1);

        assertNull(reiniciado.ultimaConsulta());
        assertNull(new ServicioMapa(archivo).ultimaConsulta());
    }

    @Test
    void rechazaUnResultadoPersistidoQueNoCorrespondeAlMapa() throws IOException {
        Path archivo = temporal.resolve("mapa.txt");
        ServicioMapa servicio = new ServicioMapa(archivo);
        servicio.agregarSede("Original");
        String anterior = Files.readString(archivo);
        Path entrada = temporal.resolve("resultado-invalido.txt");
        Files.writeString(entrada, """
                MAPA_SEDES;1
                SEDE;A
                SEDE;B
                CONEXION;A;B;4
                CONSULTA;A;B
                RESULTADO;ENCONTRADA;9;A;B
                """);

        assertThrows(IOException.class, () -> new ServicioMapa(entrada));
        assertThrows(IOException.class, () -> servicio.importar(entrada));
        assertEquals(Set.of("Original"), servicio.sedes());
        assertEquals(anterior, Files.readString(archivo));
    }
}
