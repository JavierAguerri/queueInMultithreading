package org.javieraguerri;

public class Producer implements Runnable {
    private final OrderQueue orderQueue;
    private final long delayMs;
    private volatile boolean running = true;
    private final MultithreadingManager manager;
    private final OrderFactory orderFactory;

    public Producer(OrderQueue orderQueue, long delayMs, MultithreadingManager manager, OrderFactory orderFactory) {
        this.orderQueue = orderQueue;
        this.delayMs = delayMs;
        this.manager = manager;
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
                if (!orderQueue.addOrder(orderFactory)) {
                    // Shutdown initiated or queue is full, exit loop
                    break;
                }
                Thread.sleep(delayMs);
            }
        } catch (InterruptedException e) {
            // Set the running flag to false to exit the loop
            running = false;
            Thread.currentThread().interrupt(); // Preserve interrupt status
        } finally {
            // Synchronize decrementing and logging
            int remainingProducers;
            synchronized (manager.getActiveProducers()) {
                remainingProducers = manager.getActiveProducers().decrementAndGet();
                System.out.println("Thread " + Thread.currentThread().getName() +
                        " shut down. Active producers: " + remainingProducers);
            }
        }
    }
}
