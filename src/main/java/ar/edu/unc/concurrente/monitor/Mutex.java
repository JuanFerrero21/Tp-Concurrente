package ar.edu.unc.concurrente.monitor;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Mutex {
    private final ReentrantLock lock;

    public Mutex() {
        this.lock = new ReentrantLock();
    }

    public void acquire() {
        lock.lock();
    }

    public void release() {
        lock.unlock();
    }

    public Condition newCondition() {
        return lock.newCondition();
    }
}
