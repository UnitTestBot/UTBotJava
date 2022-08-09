package org.utbot.framework.plugin.services

interface PluginService<T> {
    fun provide(): T
}