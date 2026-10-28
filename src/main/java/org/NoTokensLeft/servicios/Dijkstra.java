package org.NoTokensLeft.servicios;

import org.NoTokensLeft.dominio.MapaSedes;
import org.NoTokensLeft.dominio.ResultadoRuta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public final class Dijkstra {
    private record Entrada(String sede, long distancia) { }

    public ResultadoRuta calcular(MapaSedes mapa, String origen, String destino) {
        mapa.validarSede(origen);
        mapa.validarSede(destino);

        Map<String, Long> distancias = new HashMap<>();
        Map<String, String> anteriores = new HashMap<>();
        PriorityQueue<Entrada> pendientes = new PriorityQueue<>(
                Comparator.comparingLong(Entrada::distancia).thenComparing(Entrada::sede));
        distancias.put(origen, 0L);
        pendientes.add(new Entrada(origen, 0L));

        while (!pendientes.isEmpty()) {
            Entrada actual = pendientes.poll();
            // Una mejora puede dejar en la cola una distancia anterior para la misma sede.
            if (actual.distancia() != distancias.get(actual.sede())) {
                continue;
            }
            if (actual.sede().equals(destino)) {
                break;
            }
            for (var conexion : mapa.conexionesDesde(actual.sede()).entrySet()) {
                long candidata = actual.distancia() + conexion.getValue();
                String vecino = conexion.getKey();
                if (candidata < distancias.getOrDefault(vecino, Long.MAX_VALUE)) {
                    distancias.put(vecino, candidata);
                    anteriores.put(vecino, actual.sede());
                    pendientes.add(new Entrada(vecino, candidata));
                }
            }
        }

        if (!distancias.containsKey(destino)) {
            return ResultadoRuta.sinRuta();
        }
        ArrayList<String> ruta = new ArrayList<>();
        for (String sede = destino; sede != null; sede = anteriores.get(sede)) {
            ruta.add(sede);
        }
        Collections.reverse(ruta);
        return new ResultadoRuta(ResultadoRuta.Estado.ENCONTRADA, ruta, distancias.get(destino));
    }
}

