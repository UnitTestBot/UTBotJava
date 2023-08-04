package org.utbot.spring.utils

import org.springframework.core.GenericTypeResolver
import org.springframework.data.repository.CrudRepository

/**
 * This util class allows to obtain some data from Spring repository.
 * For example, information about entity it is working with.
 *
 * Private methods implementation is taken from https://stackoverflow.com/a/76229273.
 */
object RepositoryUtils {

    fun getEntityClass(repositoryClass: Class<*>): Class<*>? =
        getGenericType(repositoryClass, CrudRepository::class.java, 0)

    private fun getGenericType(classInstance: Class<*>, classToGetGenerics: Class<*>, genericPosition: Int): Class<*>? {
        val typeArguments = getGenericType(classInstance, classToGetGenerics)
        if (typeArguments != null && typeArguments.size >= genericPosition) {
            return typeArguments[genericPosition]
        }

        return null
    }

    private fun getGenericType(classInstance: Class<*>, classToGetGenerics: Class<*>): Array<Class<*>?>? {
        return GenericTypeResolver.resolveTypeArguments(classInstance, classToGetGenerics)
    }
}