package org.utbot.engine.simplificators

import org.utbot.engine.ArrayValue
import org.utbot.engine.MockInfoEnriched
import org.utbot.engine.ObjectValue
import org.utbot.engine.PrimitiveValue
import org.utbot.engine.StaticFieldMemoryUpdateInfo
import org.utbot.engine.SymbolicValue
import org.utbot.engine.UtFieldMockInfo
import org.utbot.engine.UtMockInfo
import org.utbot.engine.UtNewInstanceMockInfo
import org.utbot.engine.UtObjectMockInfo
import org.utbot.engine.UtStaticMethodMockInfo
import org.utbot.engine.UtStaticObjectMockInfo
import org.utbot.engine.pc.Simplificator
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.symbolic.SymbolicStateUpdate

context(Simplificator)
fun staticFieldMemoryUpdateInfo(staticFieldMemoryUpdateInfo: StaticFieldMemoryUpdateInfo) =
    with(staticFieldMemoryUpdateInfo) {
        copy(value = simplifySymbolicValue(value))
    }

context(Simplificator)
fun simplifyMockInfoEnriched(mockInfoEnriched: MockInfoEnriched): MockInfoEnriched {
    val mockInfo: UtMockInfo = simplifyUtMockInfo(mockInfoEnriched.mockInfo)
    val executables = mockInfoEnriched.executables.mapValues { (_, mockExecutableInstances) ->
        mockExecutableInstances.map { mockExecutableInstance ->
            with(mockExecutableInstance) {
                val symbolicValue = simplifySymbolicValue(value)
                copy(value = symbolicValue)
            }
        }
    }

    return MockInfoEnriched(mockInfo, executables)
}

context(Simplificator)
fun simplifyUtMockInfo(mockInfo: UtMockInfo): UtMockInfo =
    with(mockInfo) {
        when (this) {
            is UtFieldMockInfo -> copy(ownerAddr = ownerAddr?.accept(this@Simplificator) as UtAddrExpression?)
            is UtNewInstanceMockInfo -> copy(addr = addr.accept(this@Simplificator) as UtAddrExpression)
            is UtObjectMockInfo -> copy(addr = addr.accept(this@Simplificator) as UtAddrExpression)
            is UtStaticMethodMockInfo -> copy(addr = addr.accept(this@Simplificator) as UtAddrExpression)
            is UtStaticObjectMockInfo -> copy(addr = addr.accept(this@Simplificator) as UtAddrExpression)
        }
    }

context(Simplificator)
fun simplifySymbolicValue(value: SymbolicValue): SymbolicValue =
    with(value) {
        when (this) {
            is PrimitiveValue -> copy(expr = expr.accept(this@Simplificator))
            is ArrayValue -> copy(addr = addr.accept(this@Simplificator) as UtAddrExpression)
            is ObjectValue -> copy(addr = addr.accept(this@Simplificator) as UtAddrExpression)
        }
    }


context(MemoryUpdateSimplificator)
fun simplifySymbolicStateUpdate(update: SymbolicStateUpdate) =
    with(update) {
        val memoryUpdates = simplify(memoryUpdates)
        copy(memoryUpdates = memoryUpdates)
    }
