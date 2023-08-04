package org.utbot.examples.spring.utils

import org.utbot.examples.spring.autowiring.OrderRepository
import org.utbot.examples.spring.autowiring.oneBeanForOneType.Order
import org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType.Person
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


@Suppress("UNCHECKED_CAST")
val namePersonCall: KFunction1<Person, String?> =
    Person::class
        .functions
        .single { it.name == "getName" && it.parameters.size == 1 }
            as KFunction1<Person, String?>

@Suppress("UNCHECKED_CAST")
val agePersonCall: KFunction1<Person, Int?> =
    Person::class
        .functions
        .single { it.name == "getAge" && it.parameters.size == 1 }
            as KFunction1<Person, Int?>