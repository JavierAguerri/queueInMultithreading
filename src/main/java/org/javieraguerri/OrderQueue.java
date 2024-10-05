package org.javieraguerri;

import java.util.ArrayDeque;
import java.util.Queue;

public class OrderQueue {
    private final Queue<Order> queue = new ArrayDeque<>();
    private final int MAX_QUEUE_SIZE;
    private final Object lock = new Object();
    private volatile boolean shutdown = false;

    public OrderQueue(int maxQueueSize) {
        this.MAX_QUEUE_SIZE = maxQueueSize;
    }

    public boolean addOrder(OrderFactory orderFactory) throws InterruptedException {
        synchronized (lock) {
            while (queue.size() == MAX_QUEUE_SIZE && !shutdown) {
                System.out.println("Thread " + Thread.currentThread().getName() +
                        " found the queue full and goes to sleep!");
                lock.wait();
                System.out.println("Thread " + Thread.currentThread().getName() + " woke up!");
            }
            if (shutdown) {
                // No longer accepting new orders
                return false;
            }
            // Create the order here after confirming we're not shutting down
            Order order = orderFactory.produceOrder();
            queue.add(order);
            System.out.println("Thread " + Thread.currentThread().getName() +
                    " produced: " + order + " (Queue size: " + queue.size() + ")");
            lock.notifyAll();
            return true;
        }
    }

    public Order removeOrder() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty() && !shutdown) {
                System.out.println("Thread " + Thread.currentThread().getName() +
                        " found the queue empty and goes to sleep!");
                lock.wait();
                System.out.println("Thread " + Thread.currentThread().getName() + " woke up!");
            }
            if (queue.isEmpty() && shutdown) {
                return null;
            }
            Order order = queue.poll();
            System.out.println("Thread " + Thread.currentThread().getName() +
                    " consumed: " + order + " (Queue size: " + queue.size() + ")");
            lock.notifyAll();
            return order;
        }
    }

    public void shutdown() {
        synchronized (lock) {
            shutdown = true;
            lock.notifyAll();
        }
    }

    public int currentSize() {
        synchronized (lock) {
            return queue.size();
        }
    }

}
