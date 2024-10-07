package org.javieraguerri;

import lombok.SneakyThrows;

public class Producer {
    private final OrderQueue orderQueue;
    private final OrderFactory orderFactory;
    private final Thread producerThread;
    private final long delayMs;

    public Producer(OrderQueue orderQueue, long delayMs, OrderFactory orderFactory, int threadID) {
        this.orderQueue = orderQueue;
        this.delayMs = delayMs;
        this.orderFactory = orderFactory;
        this.producerThread = new Thread(this::run, "Producer-" + threadID);
        producerThread.start();
        System.out.println("Created " + producerThread.getName());
    }

    @SneakyThrows
    public int kill() {
        producerThread.interrupt();
        producerThread.join();
        return orderQueue.getTotalOrdersAdded();
    }

    public void run() {
        System.out.println(Thread.currentThread().getName() + " started");
        int i=0;
        try {
            while (true) {
                synchronized (orderQueue.getLock()) {
                    Order order = orderFactory.produceOrder(this.hashCode() + "__" + i++);
                    orderQueue.addOrder(order);
                }
                Thread.sleep(delayMs);
            }
        } catch (InterruptedException e) {
            System.out.println(Thread.currentThread().getName() + " shut down");
        }
    }
}
