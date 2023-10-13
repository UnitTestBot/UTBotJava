package org.utbot.examples.spring.autowiring.oneBeanForOneType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.utbot.examples.spring.autowiring.OrderRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceWithInjectedAndNonInjectedField {

    public List<Order> selectedOrders = new ArrayList<>();

    @Autowired
    private OrderRepository orderRepository;

    public Integer getOrdersSize() {
        return orderRepository.findAll().size() + selectedOrders.size();
    }

}
