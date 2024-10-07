package org.javieraguerri;

public class Order {
    private final String id;

    public Order(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "OrderId = " + id + "}";
    }
}
