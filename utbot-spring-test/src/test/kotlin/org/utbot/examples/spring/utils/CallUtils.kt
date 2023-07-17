package org.utbot.examples.spring.utils

import org.utbot.examples.spring.autowiring.OrderRepository
import org.utbot.examples.spring.autowiring.oneBeanForOneType.Order
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.full.functions

@Suppress("UNCHECKED_CAST")
val findAllRepositoryCall: KFunction1<OrderRepository, List<Order>?> =
    OrderRepository::class
        .functions
        .single { it.name == "findAll" && it.parameters.size == 1 }
            as KFunction1<OrderRepository, List<Order>?>


@Suppress("UNCHECKED_CAST")
val saveRepositoryCall: KFunction2<OrderRepository, Order?, Order?> =
    OrderRepository::class
        .functions
        .single { it.name == "save" && it.parameters.size == 2 }
            as KFunction2<OrderRepository, Order?, Order?>