@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package org.utbot.instrumentation.process

import org.utbot.common.withAccessibility
import sun.security.provider.PolicyFile
import java.lang.reflect.AccessibleObject
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.security.AccessControlContext
import java.security.AccessController
import java.security.CodeSource
import java.security.Permission
import java.security.PermissionCollection
import java.security.Permissions
import java.security.Policy
import java.security.PrivilegedAction
import java.security.PrivilegedActionException
import java.security.ProtectionDomain
import java.security.cert.Certificate

internal fun permissions(block: SimplePolicy.() -> Unit) {
    val policy = Policy.getPolicy()
    if (policy !is SimplePolicy) {
        Policy.setPolicy(SimplePolicy(block))
        System.setSecurityManager(SecurityManager())
    } else {
        policy.block()
    }
}

/**
 * Make this [AccessibleObject] accessible and run a block inside sandbox.
 *
 * If [bypassSandbox] is `true` then block is run without sandbox.
 */
fun <O: AccessibleObject, R> O.runSandbox(bypassSandbox: Boolean = false, block: O.() -> R): R = withAccessibility {
    if (bypassSandbox) block()
    else sandbox { block() }
}

/**
 * Run [block] in sandbox mode.
 *
 * When running in sandbox by default only necessary to instrumentation permissions are enabled.
 * Other options are not enabled by default and rises [java.security.AccessControlException].
 *
 * To add new permissions create and/or edit file "{user.home}/.utbot/sandbox.policy".
 *
 * For example to enable property reading (`System.getProperty("user.home")`):
 *
 * ```
 * grant {
 *      permission java.util.PropertyPermission "user.home", "read";
 * };
 * ```
 * Read more [about policy file and syntax](https://docs.oracle.com/javase/7/docs/technotes/guides/security/PolicyFiles.html#Examples)
 */
fun <T> sandbox(block: () -> T): T {
    val policyPath = Paths.get(System.getProperty("user.home"), ".utbot", "sandbox.policy")
    return sandbox(policyPath.toUri()) { block() }
}

fun <T> sandbox(file: URI, block: () -> T): T {
    val path = Paths.get(file)
    val perms = mutableListOf<Permission>(
        RuntimePermission("accessDeclaredMembers"),
        RuntimePermission("getStackWalkerWithClassReference"),
        RuntimePermission("getClassLoader"),
    )
    val allCodeSource = CodeSource(null, emptyArray<Certificate>())
    if (Files.exists(path)) {
        val policyFile = PolicyFile(file.toURL())
        val collection = policyFile.getPermissions(allCodeSource)
        perms += collection.elements().toList()
    }
    return sandbox(perms, allCodeSource) { block() }
}

fun <T> sandbox(permission: List<Permission>, cs: CodeSource, block: () -> T): T {
    val perms = permission.fold(Permissions()) { acc, p -> acc.add(p); acc }
    return sandbox(perms, cs) { block() }
}

fun <T> sandbox(perms: PermissionCollection, cs: CodeSource, block: () -> T): T {
    val acc = AccessControlContext(arrayOf(ProtectionDomain(cs, perms)))
    return try {
        AccessController.doPrivileged(PrivilegedAction { block() }, acc)
    } catch (e: PrivilegedActionException) {
        throw e.exception
    }
}

/**
 * This policy can add grant or denial rules for permissions.
 *
 * To add a grant permission use like this in any place:
 *
 * ```
 * permissions {
 *   + java.security.PropertyPolicy("user.home", "read,write")
 * }
 * ```
 *
 * After first call [SecurityManager] is set with this policy
 *
 * To deny a permission:
 *
 * ```
 * permissions {
 *   - java.security.PropertyPolicy("user.home", "read,write")
 * }
 * ```
 *
 * To delete all concrete permissions (if it was added before):
 *
 * ```
 * permissions {
 *   ! java.security.PropertyPolicy("user.home", "read,write")
 * }
 * ```
 *
 * The last permission has priority. Enable all property read for "user.*", but forbid to read only "user.home":
 *
 * ```
 * permissions {
 *   + java.security.PropertyPolicy("user.*", "read,write")
 *   - java.security.PropertyPolicy("user.home", "read,write")
 * }
 * ```
 */
internal class SimplePolicy(init: SimplePolicy.() -> Unit = {}) : Policy() {
    sealed class Access(val permission: Permission) {
        class Allow(permission: Permission) : Access(permission)
        class Deny(permission: Permission) : Access(permission)
    }
    private var permissions = mutableListOf<Access>()

    init { apply(init) }

    operator fun Permission.unaryPlus() = permissions.add(Access.Allow(this))

    operator fun Permission.unaryMinus() = permissions.add(Access.Deny(this))

    operator fun Permission.not() = permissions.removeAll { it.permission == this }

    override fun getPermissions(codesource: CodeSource) = UNSUPPORTED_EMPTY_COLLECTION!!
    override fun getPermissions(domain: ProtectionDomain) = UNSUPPORTED_EMPTY_COLLECTION!!
    override fun implies(domain: ProtectionDomain, permission: Permission): Boolean {
        // 0 means no info, < 0 is denied and > 0 is allowed
        val result = permissions.lastOrNull { it.permission.implies(permission) }?.let {
            when (it) {
                is Access.Allow -> 1
                is Access.Deny -> -1
            }
        } ?: 0
        return result > 0
    }
}