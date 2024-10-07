package org.javieraguerri;

import lombok.SneakyThrows;

public class Consumer {
    private final OrderQueue orderQueue;
    private final OrderProcessor orderProcessor;
    private final Thread consumerThread;
    private final long delayMs;

    public Consumer(OrderQueue orderQueue, long delayMs, OrderProcessor orderProcessor, int threadID) {
        this.orderQueue = orderQueue;
        this.delayMs = delayMs;
        this.orderProcessor = orderProcessor;
        this.consumerThread = new Thread(this::run, "Consumer-" + threadID);
        consumerThread.start();
        System.out.println("Created " + consumerThread.getName());
    }

    @SneakyThrows
    public void shutdown() {
        consumerThread.join();
    }

    @SneakyThrows
    public void kill() {
        consumerThread.interrupt();
    }

    public void run() {
        System.out.println(Thread.currentThread().getName() + " started");
        try {
            while (true) {
                synchronized (orderQueue.getLock()) {
                    Order order = orderQueue.removeOrder();
                    if (order == null)
                        break;
                    orderProcessor.processOrder(order);
                }
                Thread.sleep(delayMs);
            }
        } catch (InterruptedException e) {
            System.out.println(Thread.currentThread().getName() + " shut down");
        }
    }
}
