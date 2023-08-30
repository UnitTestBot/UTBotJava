package org.utbot.common

import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.util.*

/**
 * [ClassLoader] implementation, that
 *   - first, attempts to load class/resource with [commonParent] class loader
 *   - next, attempts to load class/resource from `urls`
 *   - finally, attempts to load class/resource with `fallback` class loader
 *
 * More details can be found in [this post](https://medium.com/@isuru89/java-a-child-first-class-loader-cbd9c3d0305).
 */
class FallbackClassLoader(
    urls: Array<URL>,
    fallback: ClassLoader,
    private val commonParent: ClassLoader = fallback.parent,
) : URLClassLoader(urls, fallback) {

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*>? {
        // has the class loaded already?
        var loadedClass = findLoadedClass(name)
        if (loadedClass == null) {
            try {
                loadedClass = commonParent.loadClass(name)
            } catch (ex: ClassNotFoundException) {
                // class not found in common parent loader... silently skipping
            }
            try {
                // find the class from given jar urls as in first constructor parameter.
                if (loadedClass == null) {
                    loadedClass = findClass(name)
                }
            } catch (e: ClassNotFoundException) {
                // class is not found in the given urls.
                // Let's try it in fallback classloader.
                // If class is still not found, then this method will throw class not found ex.
                loadedClass = super.loadClass(name, resolve)
            }
        }
        if (resolve) {      // marked to resolve
            resolveClass(loadedClass)
        }
        return loadedClass
    }

    @Throws(IOException::class)
    override fun getResources(name: String): Enumeration<URL> {
        val allRes: MutableList<URL> = LinkedList<URL>()

        // load resources from common parent loader
        val commonParentResources: Enumeration<URL>? = commonParent.getResources(name)
        if (commonParentResources != null) {
            while (commonParentResources.hasMoreElements()) {
                allRes.add(commonParentResources.nextElement())
            }
        }

        // load resource from this classloader
        val thisRes: Enumeration<URL>? = findResources(name)
        if (thisRes != null) {
            while (thisRes.hasMoreElements()) {
                allRes.add(thisRes.nextElement())
            }
        }

        // then try finding resources from fallback classloaders
        val parentRes: Enumeration<URL>? = super.findResources(name)
        if (parentRes != null) {
            while (parentRes.hasMoreElements()) {
                allRes.add(parentRes.nextElement())
            }
        }
        return object : Enumeration<URL> {
            var it: Iterator<URL> = allRes.iterator()
            override fun hasMoreElements(): Boolean {
                return it.hasNext()
            }

            override fun nextElement(): URL {
                return it.next()
            }
        }
    }

    override fun getResource(name: String): URL? {
        var res: URL? = commonParent.getResource(name)
        if (res === null) {
            res = findResource(name)
        }
        if (res === null) {
            res = super.getResource(name)
        }
        return res
    }
}