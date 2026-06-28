package ar.edu.unc.concurrente.politica;

import java.util.Random;
import java.util.Set;

public class PoliticaAleatoria implements Politica {
    private final Random aleatorio;

    public PoliticaAleatoria() {
        this.aleatorio = new Random();
    }

    @Override
    public int elegirTransicion(Set<Integer> transicionesSensibilizadas) {
        if (transicionesSensibilizadas.isEmpty()) {
            throw new IllegalArgumentException("No hay transiciones habilitadas para elegir");
        }

        int indiceSeleccionado = aleatorio.nextInt(transicionesSensibilizadas.size());
        int indice = 0;
        for (int transicion : transicionesSensibilizadas) {
            if (indice == indiceSeleccionado) {
                return transicion;
            }
            indice++;
        }

        throw new IllegalStateException("No se pudo seleccionar una transicion");
    }
}
