package org.utbot.framework.codegen.model.util

/**
 * Creates a name of test class.
 * We need the name in code and the name of test class file be similar.
 * On this way we need to avoid symbols like '$'.
 */
fun createTestClassName(name: String): String = name
    .substringAfterLast('.')
    .replace('\$', '_')