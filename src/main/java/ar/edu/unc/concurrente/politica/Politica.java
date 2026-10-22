package ar.edu.unc.concurrente.politica;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public interface Politica {
    int elegirTransicion(Set<Integer> transicionesSensibilizadas);

    /*
     * Un Set no tiene posiciones (no existe un "set.get(indice)"), asi que
     * para elegir un elemento al azar primero lo volcamos a una lista.
     * Logica compartida por las politicas que necesiten un sorteo uniforme.
     */
    static int elegirAlAzar(Set<Integer> transicionesSensibilizadas, Random aleatorio) {
        if (transicionesSensibilizadas.isEmpty()) {
            throw new IllegalArgumentException("No hay transiciones habilitadas para elegir");
        }

        List<Integer> candidatas = new ArrayList<>(transicionesSensibilizadas);
        return candidatas.get(aleatorio.nextInt(candidatas.size()));
    }
}
