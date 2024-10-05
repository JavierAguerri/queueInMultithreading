package org.javieraguerri;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderFactory {
    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicInteger totalOrdersCreated = new AtomicInteger(0);

    public Order produceOrder() {
        int id = counter.incrementAndGet();
        totalOrdersCreated.incrementAndGet();
        return new Order(id);
    }

    public int getTotalOrdersCreated() {
        return totalOrdersCreated.get();
    }
}
