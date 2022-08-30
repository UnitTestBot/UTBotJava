package org.utbot.common

private val javaSpecificationVersion = System.getProperty("java.specification.version")
val isJvm8 = javaSpecificationVersion.equals("1.8")
val isJvm9Plus = !javaSpecificationVersion.contains(".") && javaSpecificationVersion.toInt() >= 9
