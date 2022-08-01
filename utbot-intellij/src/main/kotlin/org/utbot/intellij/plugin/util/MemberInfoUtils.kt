package org.utbot.intellij.plugin.util

import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.refactoring.classMembers.MemberInfoBase

val MemberInfoBase<out PsiModifierListOwner>.isAbstract: Boolean
    get() = this.member.modifierList?.hasModifierProperty(PsiModifier.ABSTRACT)?: false