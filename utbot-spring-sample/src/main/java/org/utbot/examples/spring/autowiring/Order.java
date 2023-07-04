package org.utbot.examples.spring.autowiring;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import javax.persistence.*;

@Getter
@Setter
@Builder
@ToString
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String buyer;
    Double price;
    int qty;
}
