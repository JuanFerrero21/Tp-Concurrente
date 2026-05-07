package ar.edu.unc.concurrente.monitor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class TransitionQueues {
    private final Condition[] conditionsByTransition;
    private final int[] waitingByTransition;

    public TransitionQueues(Mutex mutex, int transitionCount) {
        this.conditionsByTransition = new Condition[transitionCount];
        this.waitingByTransition = new int[transitionCount];

        for (int i = 0; i < transitionCount; i++) {
            conditionsByTransition[i] = mutex.newCondition();
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

    public void awaitMillis(int transition, long millis) throws InterruptedException {
        if (millis <= 0) {
            return;
        }

        conditionsByTransition[transition].await(millis, TimeUnit.MILLISECONDS);
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