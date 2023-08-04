package org.utbot.examples.spring.autowiring.oneBeanForOneType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.utbot.examples.spring.autowiring.OrderRepository;

import java.util.List;

@Service
public class ServiceWithInjectedField {

    @Autowired
    private OrderRepository orderRepository;

    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }
}
