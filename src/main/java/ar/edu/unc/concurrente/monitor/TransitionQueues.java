package ar.edu.unc.concurrente.monitor;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TransitionQueues {
    private final Condition[] conditionsByTransition;
    private final int[] waitingByTransition;

    public TransitionQueues(Lock lock, int transitionCount) {
        this.conditionsByTransition = new Condition[transitionCount];
        this.waitingByTransition = new int[transitionCount];

        for (int i = 0; i < transitionCount; i++) {
            conditionsByTransition[i] = lock.newCondition();
        }
    }

    public void startWaiting(int transition) {
        waitingByTransition[transition]++;
    }

    public void stopWaiting(int transition) {
        waitingByTransition[transition]--;
    }

    public boolean hasWaitingThread(int transition) {
        return waitingByTransition[transition] > 0;
    }

    public int getTransitionCount() {
        return waitingByTransition.length;
    }

    public void await(int transition) throws InterruptedException {
        conditionsByTransition[transition].await();
    }

    public void signal(int transition) {
        conditionsByTransition[transition].signal();
    }

    public void signalAll() {
        for (Condition condition : conditionsByTransition) {
            condition.signalAll();
        }
    }
}
