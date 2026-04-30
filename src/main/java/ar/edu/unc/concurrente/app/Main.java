package ar.edu.unc.concurrente.app;

import ar.edu.unc.concurrente.monitor.Monitor;
import ar.edu.unc.concurrente.monitor.MonitorInterface;
import ar.edu.unc.concurrente.petri.Marking;
import ar.edu.unc.concurrente.petri.PetriNet;
import ar.edu.unc.concurrente.threads.WorkerThread;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int[][] incidenceMatrix = {
                {-1, 1},
                {1, -1}
        };

        PetriNet petriNet = new PetriNet(new Marking(new int[] {1, 0}), incidenceMatrix);
        MonitorInterface monitor = new Monitor(petriNet);

        System.out.println("Marcado inicial: " + petriNet.getMarking());
        System.out.println("Transiciones sensibilizadas: " + petriNet.getEnabledTransitions());
        WorkerThread worker = new WorkerThread("Worker-1", monitor, new int[] {0, 1}, 4);

        worker.start();
        worker.join();

        System.out.println("Marcado final: " + petriNet.getMarking());
    }
}
