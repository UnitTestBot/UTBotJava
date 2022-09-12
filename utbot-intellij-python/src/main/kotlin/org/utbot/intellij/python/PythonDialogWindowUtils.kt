package org.utbot.intellij.python

import com.intellij.psi.PsiElement
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.ui.AbstractMemberSelectionTable
import com.jetbrains.python.refactoring.classes.ui.PyMemberSelectionTable

object PythonDialogWindowUtils {
    fun initFunctionsTable(): AbstractMemberSelectionTable<PsiElement, MemberInfoBase<PsiElement>> {
        return PyMemberSelectionTable(emptyList(), null, false) as AbstractMemberSelectionTable<PsiElement, MemberInfoBase<PsiElement>>
    }
}