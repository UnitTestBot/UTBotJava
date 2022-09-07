package org.utbot.instrumentation.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.utbot.instrumentation.process.permissions
import org.utbot.instrumentation.process.sandbox
import java.lang.NullPointerException
import java.security.AccessControlException
import java.security.AllPermission
import java.security.BasicPermission
import java.security.CodeSource
import java.security.Permission
import java.security.cert.Certificate
import java.util.PropertyPermission

class SecurityTest {

    @BeforeEach
    fun init() {
        permissions {
            +AllPermission()
        }
    }

    @Test
    fun `basic security works`() {
        sandbox {
            assertThrows<AccessControlException> {
                System.getProperty("any")
            }
        }
    }

    @Test
    fun `basic permission works`() {
        sandbox(listOf(PropertyPermission("java.version", "read")), CodeSource(null, emptyArray<Certificate>())) {
            val result = System.getProperty("java.version")
            assertNotNull(result)
            assertThrows<AccessControlException> {
                System.setProperty("any_random_value_key", "random")
            }
        }
    }

    @Test
    fun `null is ok`() {
        val empty = object : BasicPermission("*") {}
        val field = Permission::class.java.getDeclaredField("name")
        field.isAccessible = true
        field.set(empty, null)
        val collection = empty.newPermissionCollection()
        assertThrows<NullPointerException> {
            collection.implies(empty)
        }
    }

}