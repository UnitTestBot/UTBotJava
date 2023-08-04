package org.utbot.examples.spring.autowiring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.utbot.examples.spring.autowiring.oneBeanForOneType.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
