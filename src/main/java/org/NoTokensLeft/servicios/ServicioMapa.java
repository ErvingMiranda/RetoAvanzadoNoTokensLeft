package org.NoTokensLeft.servicios;

import org.NoTokensLeft.dominio.ConsultaRuta;
import org.NoTokensLeft.dominio.MapaSedes;
import org.NoTokensLeft.dominio.ResultadoRuta;
import org.NoTokensLeft.persistencia.RepositorioMapaArchivo;
import org.NoTokensLeft.persistencia.RepositorioMapaArchivo.EstadoGuardado;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class ServicioMapa {
    private final Path archivo;
    private final RepositorioMapaArchivo repositorio = new RepositorioMapaArchivo();
    private final Dijkstra dijkstra = new Dijkstra();
    private EstadoGuardado estado;

    public ServicioMapa(Path archivo) throws IOException {
        this.archivo = archivo;
        estado = Files.notExists(archivo) ? new EstadoGuardado(new MapaSedes(), null) : repositorio.cargar(archivo);
        validarConsulta(estado);
    }

    public void agregarSede(String nombre) throws IOException {
        MapaSedes candidato = copiarMapa();
        candidato.agregarSede(nombre);
        guardarCambio(new EstadoGuardado(candidato, null));
    }

    public void conectar(String origen, String destino, int costo) throws IOException {
        MapaSedes candidato = copiarMapa();
        candidato.conectar(origen, destino, costo);
        guardarCambio(new EstadoGuardado(candidato, null));
    }

    public ResultadoRuta calcularRuta(String origen, String destino) throws IOException {
        ResultadoRuta resultado = dijkstra.calcular(estado.mapa(), origen, destino);
        guardarCambio(new EstadoGuardado(estado.mapa(), new ConsultaRuta(origen, destino, resultado)));
        return resultado;
    }

    public ConsultaRuta ultimaConsulta() {
        return estado.ultimaConsulta();
    }

    public Set<String> sedes() {
        return estado.mapa().sedes();
    }

    public Map<String, Integer> conexionesDesde(String origen) {
        return estado.mapa().conexionesDesde(origen);
    }

    public void importar(Path entrada) throws IOException {
        EstadoGuardado candidato = repositorio.cargar(entrada);
        validarConsulta(candidato);
        guardarCambio(candidato);
    }

    public void exportar(Path destino) throws IOException {
        repositorio.guardar(estado, destino);
    }

    private void guardarCambio(EstadoGuardado candidato) throws IOException {
        // La memoria solo cambia después de confirmar el guardado en disco.
        repositorio.guardar(candidato, archivo);
        estado = candidato;
    }

    private MapaSedes copiarMapa() {
        MapaSedes mapa = estado.mapa();
        MapaSedes copia = new MapaSedes();
        for (String sede : mapa.sedes()) {
            copia.agregarSede(sede);
        }
        for (String origen : mapa.sedes()) {
            mapa.conexionesDesde(origen).forEach((destino, costo) -> copia.conectar(origen, destino, costo));
        }
        return copia;
    }

    private void validarConsulta(EstadoGuardado candidato) throws IOException {
        ConsultaRuta consulta = candidato.ultimaConsulta();
        if (consulta != null) {
            ResultadoRuta esperado = dijkstra.calcular(candidato.mapa(), consulta.origen(), consulta.destino());
            if (!esperado.equals(consulta.resultado())) {
                throw new IOException("El resultado guardado no coincide con la ruta calculada para este mapa.");
            }
        }
    }
}
