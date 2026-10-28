package org.NoTokensLeft.dominio;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// Responsable: Kayro - sedes y conexiones dirigidas válidas.
public final class MapaSedes {
    private final Map<String, Map<String, Integer>> conexiones = new LinkedHashMap<>();

    public void agregarSede(String nombre) {
        if (nombre == null || nombre.isBlank() || !nombre.equals(nombre.strip())
                || nombre.contains(";") || nombre.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "El nombre de sede no puede estar vacío ni contener ';', controles o espacios en los extremos.");
        }
        if (conexiones.containsKey(nombre)) {
            throw new IllegalArgumentException("La sede ya existe: " + nombre);
        }
        conexiones.put(nombre, new LinkedHashMap<>());
    }

    public void conectar(String origen, String destino, int costo) {
        validarSede(origen);
        validarSede(destino);
        if (costo < 0) {
            throw new IllegalArgumentException("El costo no puede ser negativo.");
        }
        conexiones.get(origen).put(destino, costo);
    }

    public void validarSede(String nombre) {
        if (!conexiones.containsKey(nombre)) {
            throw new IllegalArgumentException("La sede no existe: " + nombre);
        }
    }

    public Set<String> sedes() {
        return Collections.unmodifiableSet(conexiones.keySet());
    }

    public Map<String, Integer> conexionesDesde(String origen) {
        validarSede(origen);
        return Collections.unmodifiableMap(conexiones.get(origen));
    }
}
