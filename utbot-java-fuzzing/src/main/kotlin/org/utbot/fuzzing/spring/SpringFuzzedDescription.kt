package org.utbot.fuzzing.spring

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.Instruction
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.ScopeParams
import org.utbot.fuzzing.utils.Trie
import java.lang.reflect.Type
import kotlin.random.Random

class SpringFuzzedDescription(
    description: FuzzedMethodDescription,
    tracer: Trie<Instruction, *>,
    typeCache: MutableMap<Type, FuzzedType>,
    random: Random,
    scope: ScopeParams? = null,
    private val beanNamesFinder: (ClassId) -> List<String>,
) : FuzzedDescription(description, tracer, typeCache, random, scope) {

    constructor(
        javaFuzzedDescription: FuzzedDescription,
        beanNamesFinder: (ClassId) -> List<String>,
    ) : this(
        javaFuzzedDescription.description,
        javaFuzzedDescription.tracer,
        javaFuzzedDescription.typeCache,
        javaFuzzedDescription.random,
        javaFuzzedDescription.scope,
        beanNamesFinder
    )

    constructor(
        description: FuzzedMethodDescription,
        tracer: Trie<Instruction, *>,
        typeCache: MutableMap<Type, FuzzedType>,
        random: Random,
        scope: ScopeParams? = null,
        beanClassId: ClassId,
        beanNames: List<String>,
    ) : this(description, tracer, typeCache, random, scope, { cid -> beanNames.takeIf { cid == beanClassId } ?: emptyList() })

    override fun fork(type: FuzzedType, scope: ScopeParams): FuzzedDescription? {
        if (checkParamIsThis() == true) {
            val beans = findBeans(type.classId)
            if (beans.isNotEmpty()) {
                return SpringFuzzedDescription(description, tracer, typeCache, random, scope, type.classId, beans)
            }
        }
        return super.fork(type, scope)
    }

    fun findBeans(classId: ClassId) : List<String> = beanNamesFinder.invoke(classId)

}