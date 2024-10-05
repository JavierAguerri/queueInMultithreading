package org.javieraguerri;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderFactory {
    private final AtomicInteger totalOrdersCreated = new AtomicInteger(0);

    public Order produceOrder() {
        return new Order(totalOrdersCreated.incrementAndGet());
    }

    public int getTotalOrdersCreated() {
        return totalOrdersCreated.get();
    }
}
