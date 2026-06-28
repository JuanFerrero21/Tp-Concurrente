package ar.edu.unc.concurrente.hilo;

import ar.edu.unc.concurrente.analisis.EstadoSimulacion;
import ar.edu.unc.concurrente.registro.RegistradorTransiciones;
import ar.edu.unc.concurrente.monitor.MonitorInterface;

public class Hilo extends Thread {
    private final MonitorInterface monitor;
    private final int[] transicionesAsignadas;
    private final EstadoSimulacion estadoSimulacion;
    private final RegistradorTransiciones registradorTransiciones;

    public Hilo(
            String nombre,
            MonitorInterface monitor,
            int[] transicionesAsignadas,
            EstadoSimulacion estadoSimulacion,
            RegistradorTransiciones registradorTransiciones
    ) {
        super(nombre);
        this.monitor = monitor;
        this.transicionesAsignadas = transicionesAsignadas;
        this.estadoSimulacion = estadoSimulacion;
        this.registradorTransiciones = registradorTransiciones;
    }

    @Override
    public void run() {
        while (!estadoSimulacion.estaFinalizada()) {
            for (int transicion : transicionesAsignadas) {
                boolean disparada = monitor.fireTransition(transicion);

                if (registradorTransiciones != null) {
                    registradorTransiciones.registrarIntento(transicion, disparada);
                }

                if (!disparada) {
                    return;
                }
            }
        }
    }
}
