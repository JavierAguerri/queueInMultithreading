package org.javieraguerri;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderProcessor {
    private final AtomicInteger totalOrdersProcessed = new AtomicInteger(0);

    public int getTotalOrdersProcessed() {
        return totalOrdersProcessed.get();
    }

    public void processOrder(Order order) {
        System.out.println("Thread " + Thread.currentThread().getName() + " processing " + order);
        totalOrdersProcessed.incrementAndGet();
    }
}
