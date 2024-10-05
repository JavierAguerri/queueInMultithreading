package org.javieraguerri;

public class Order {
    private final int id;

    public Order(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "OrderId = " + id + "}";
    }
}
