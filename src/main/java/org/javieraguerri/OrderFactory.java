package org.javieraguerri;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderFactory {
    private final AtomicInteger totalOrdersCreated = new AtomicInteger(0);

    public Order produceOrder() {
        return new Order(totalOrdersCreated.incrementAndGet());
    }

    public int getTotalOrdersCreated() {
        return totalOrdersCreated.get();
    }
}
