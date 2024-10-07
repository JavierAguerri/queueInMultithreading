package org.javieraguerri;

import org.springframework.stereotype.Component;

@Component
public class OrderFactory {
    public Order produceOrder(String id) {
        return new Order(id);
    }
}
