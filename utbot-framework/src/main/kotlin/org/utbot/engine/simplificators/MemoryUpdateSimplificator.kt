package org.utbot.engine.simplificators

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import org.utbot.engine.Concrete
import org.utbot.engine.InstanceFieldReadOperation
import org.utbot.engine.MemoryChunkDescriptor
import org.utbot.engine.MemoryUpdate
import org.utbot.engine.MockInfoEnriched
import org.utbot.engine.ObjectValue
import org.utbot.engine.StaticFieldMemoryUpdateInfo
import org.utbot.engine.SymbolicValue
import org.utbot.engine.UtMockInfo
import org.utbot.engine.UtNamedStore
import org.utbot.engine.pc.Simplificator
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtBoolExpression
import org.utbot.engine.pc.UtExpression
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import soot.ArrayType
import soot.SootField

typealias StoresType = PersistentList<UtNamedStore>
typealias TouchedChunkDescriptorsType = PersistentSet<MemoryChunkDescriptor>
typealias ConcreteType = PersistentMap<UtAddrExpression, Concrete>
typealias MockInfosType = PersistentList<MockInfoEnriched>
typealias StaticInstanceStorageType = PersistentMap<ClassId, ObjectValue>
typealias InitializedStaticFieldsType = PersistentSet<FieldId>
typealias StaticFieldsUpdatesType = PersistentList<StaticFieldMemoryUpdateInfo>
typealias MeaningfulStaticFieldsType = PersistentSet<FieldId>
typealias FieldValuesType = PersistentMap<SootField, PersistentMap<UtAddrExpression, SymbolicValue>>
typealias AddrToArrayTypeType = PersistentMap<UtAddrExpression, ArrayType>
typealias AddrToMockInfoType = PersistentMap<UtAddrExpression, UtMockInfo>
typealias VisitedValuesType = PersistentList<UtAddrExpression>
typealias TouchedAddressesType = PersistentList<UtAddrExpression>
typealias ClassIdToClearStaticsType = ClassId?
typealias InstanceFieldReadsType = PersistentSet<InstanceFieldReadOperation>
typealias SpeculativelyNotNullAddressesType = PersistentList<UtAddrExpression>
typealias SymbolicEnumValuesType = PersistentList<ObjectValue>
typealias TaintArrayUpdateType = PersistentList<Pair<UtAddrExpression, UtExpression>>
typealias TaintAnalysisFoundSomethingType = UtBoolExpression

class MemoryUpdateSimplificator(
    private val simplificator: Simplificator
) : CachingSimplificatorAdapter<MemoryUpdate>() {
    override fun simplifyImpl(expression: MemoryUpdate): MemoryUpdate = with(expression) {
        val stores = simplifyStores(stores)
        val touchedChunkDescriptors = simplifyTouchedChunkDescriptors(touchedChunkDescriptors)
        val concrete = simplifyConcrete(concrete)
        val mockInfos = simplifyMockInfos(mockInfos)
        val staticInstanceStorage = simplifyStaticInstanceStorage(staticInstanceStorage)
        val initializedStaticFields = simplifyInitializedStaticFields(initializedStaticFields)
        val staticFieldsUpdates = simplifyStaticFieldsUpdates(staticFieldsUpdates)
        val meaningfulStaticFields = simplifyMeaningfulStaticFields(meaningfulStaticFields)
        val fieldValues = simplifyFieldValues(fieldValues)
        val addrToArrayType = simplifyAddrToArrayType(addrToArrayType)
        val addrToMockInfo = simplifyAddrToMockInfo(addrToMockInfo)
        val visitedValues = simplifyVisitedValues(visitedValues)
        val touchedAddresses = simplifyTouchedAddresses(touchedAddresses)
        val classIdToClearStatics = simplifyClassIdToClearStatics(classIdToClearStatics)
        val instanceFieldReads = simplifyInstanceFieldReads(instanceFieldReads)
        val speculativelyNotNullAddresses =
            simplifySpeculativelyNotNullAddresses(speculativelyNotNullAddresses)
        val symbolicEnumValues = simplifyEnumValues(symbolicEnumValues)
        val taintArrayUpdate = simplifyTaintArrayUpdate(taintArrayUpdate)
        val taintAnalysisFoundSomething = simplifyTaintFoundSomething(taintAnalysisFoundSomething)

        return MemoryUpdate(
            stores,
            touchedChunkDescriptors,
            concrete,
            mockInfos,
            staticInstanceStorage,
            initializedStaticFields,
            staticFieldsUpdates,
            meaningfulStaticFields,
            fieldValues,
            addrToArrayType,
            addrToMockInfo,
            visitedValues,
            touchedAddresses,
            classIdToClearStatics,
            instanceFieldReads,
            speculativelyNotNullAddresses,
            taintArrayUpdate,
            taintAnalysisFoundSomething,
            symbolicEnumValues
        )
    }

    private fun simplifyStores(stores: StoresType): StoresType =
        stores
            .mutate { prevStores ->
                prevStores.replaceAll { store ->
                    store.copy(
                        index = store.index.accept(simplificator),
                        value = store.value.accept(simplificator)
                    )
                }
            }

    private fun simplifyTouchedChunkDescriptors(touchedChunkDescriptors: TouchedChunkDescriptorsType): TouchedChunkDescriptorsType =
        touchedChunkDescriptors

    private fun simplifyConcrete(concrete: ConcreteType): ConcreteType =
        concrete
            .mapKeys { (k, _) -> k.accept(simplificator) as UtAddrExpression }
            .toPersistentMap()

    private fun simplifyMockInfos(mockInfos: MockInfosType): MockInfosType =
        mockInfos.mutate { prevMockInfos ->
            prevMockInfos.replaceAll {
                with(simplificator) {
                    simplifyMockInfoEnriched(it)
                }
            }
        }


    private fun simplifyStaticInstanceStorage(staticInstanceStorage: StaticInstanceStorageType): StaticInstanceStorageType =
        staticInstanceStorage.mutate { prevStorage ->
            prevStorage.replaceAll { _, v -> with(simplificator) { simplifySymbolicValue(v) as ObjectValue } }
        }

    private fun simplifyInitializedStaticFields(initializedStaticFields: InitializedStaticFieldsType): InitializedStaticFieldsType =
        initializedStaticFields

    private fun simplifyStaticFieldsUpdates(staticFieldsUpdates: StaticFieldsUpdatesType): StaticFieldsUpdatesType =
        staticFieldsUpdates.mutate { prevUpdates ->
            prevUpdates.replaceAll { with(simplificator) { staticFieldMemoryUpdateInfo(it) } }
        }

    private fun simplifyMeaningfulStaticFields(meaningfulStaticFields: MeaningfulStaticFieldsType): MeaningfulStaticFieldsType =
        meaningfulStaticFields

    private fun simplifyFieldValues(fieldValues: FieldValuesType): FieldValuesType = fieldValues

    private fun simplifyAddrToArrayType(addrToArrayType: AddrToArrayTypeType): AddrToArrayTypeType =
        addrToArrayType
            .mapKeys { (k, _) -> k.accept(simplificator) as UtAddrExpression }
            .toPersistentMap()


    private fun simplifyAddrToMockInfo(addrToMockInfo: AddrToMockInfoType): AddrToMockInfoType =
        addrToMockInfo
            .mapKeys { (k, _) -> k.accept(simplificator) as UtAddrExpression }
            .toPersistentMap()

    private fun simplifyVisitedValues(visitedValues: VisitedValuesType): VisitedValuesType =
        visitedValues.mutate { prevValues ->
            prevValues.replaceAll { it.accept(simplificator) as UtAddrExpression }
        }

    private fun simplifyTouchedAddresses(touchedAddresses: TouchedAddressesType): TouchedAddressesType =
        touchedAddresses.mutate { prevAddresses ->
            prevAddresses.replaceAll { it.accept(simplificator) as UtAddrExpression }
        }

    private fun simplifyClassIdToClearStatics(classIdToClearStatics: ClassIdToClearStaticsType): ClassIdToClearStaticsType =
        classIdToClearStatics

    private fun simplifyInstanceFieldReads(instanceFieldReads: InstanceFieldReadsType): InstanceFieldReadsType =
        instanceFieldReads
            .map { it.copy(addr = it.addr.accept(simplificator) as UtAddrExpression) }
            .toPersistentSet()

    private fun simplifySpeculativelyNotNullAddresses(speculativelyNotNullAddresses: SpeculativelyNotNullAddressesType): SpeculativelyNotNullAddressesType =
        speculativelyNotNullAddresses.mutate { prevAddresses ->
            prevAddresses.replaceAll { it.accept(simplificator) as UtAddrExpression }
        }

    private fun simplifyEnumValues(symbolicEnumValues: SymbolicEnumValuesType): SymbolicEnumValuesType =
        symbolicEnumValues.mutate { values ->
            values.replaceAll { with(simplificator) { simplifySymbolicValue(it) as ObjectValue } }
        }

    private fun simplifyTaintArrayUpdate(taintArrayUpdate: TaintArrayUpdateType): TaintArrayUpdateType =
        taintArrayUpdate.mutate { values ->
            values.replaceAll {
                val simplifiedAddr = it.first.accept(simplificator) as UtAddrExpression
                val simplifiedExpr = it.second.accept(simplificator)

                simplifiedAddr to simplifiedExpr
            }
        }


    private fun simplifyTaintFoundSomething(
        taintAnalysisFoundSomethingType: TaintAnalysisFoundSomethingType
    ): TaintAnalysisFoundSomethingType =
        taintAnalysisFoundSomethingType.accept(simplificator) as UtBoolExpression
}
