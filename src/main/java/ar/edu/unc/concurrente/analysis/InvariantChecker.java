package ar.edu.unc.concurrente.analysis;

import ar.edu.unc.concurrente.petri.Marking;

public class InvariantChecker {
    public boolean keepsTokenTotal(Marking initialMarking, Marking finalMarking) {
        return sum(initialMarking) == sum(finalMarking);
    }

    public int sum(Marking marking) {
        int total = 0;
        for (int token : marking.toArray()) {
            total += token;
        }

        return total;
    }
}
