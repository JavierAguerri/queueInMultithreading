package org.javieraguerri;

public class Consumer implements Runnable {
    private final OrderQueue orderQueue;
    private final long delayMs;
    private final OrderProcessor orderProcessor;

    public Consumer(OrderQueue orderQueue, long delayMs, OrderProcessor orderProcessor) {
        this.orderQueue = orderQueue;
        this.delayMs = delayMs;
        this.orderProcessor = orderProcessor;
    }

    @Override
    public void run() {
        System.out.println("Thread " + Thread.currentThread().getName() + " started.");
        try {
            while (true) {
                Order order = orderQueue.removeOrder();
                if (order == null) break;
                orderProcessor.processOrder(order);
                Thread.sleep(delayMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("Thread " + Thread.currentThread().getName() + " shut down");
        }
    }
}
