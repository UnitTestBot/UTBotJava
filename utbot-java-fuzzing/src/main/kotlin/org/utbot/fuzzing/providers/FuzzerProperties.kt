package org.utbot.fuzzing.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.fuzzing.ScopeProperty

val NULLABLE_PROP = ScopeProperty<Boolean>("Whether or not type can be nullable")

val SPRING_BEAN_PROP = ScopeProperty<(ClassId) -> List<String>>("Maps java class to its beans")