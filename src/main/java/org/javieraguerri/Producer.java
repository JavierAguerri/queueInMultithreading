package org.javieraguerri;

public class Producer implements Runnable {
    private final OrderQueue orderQueue;
    private final long delayMs;
    private volatile boolean running = true;
    private final OrderFactory orderFactory;

    public Producer(OrderQueue orderQueue, long delayMs, OrderFactory orderFactory) {
        this.orderQueue = orderQueue;
        this.delayMs = delayMs;
        this.orderFactory = orderFactory;
    }

    public void shutdown() {
        running = false;
    }

    @Override
    public void run() {
        System.out.println("Thread " + Thread.currentThread().getName() + " started.");
        try {
            while (running) {
                if (!orderQueue.addOrder(orderFactory)) break;
                Thread.sleep(delayMs);
            }
        } catch (InterruptedException e) {
            running = false;
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("Thread " + Thread.currentThread().getName() + " shut down.");
        }
    }
}
