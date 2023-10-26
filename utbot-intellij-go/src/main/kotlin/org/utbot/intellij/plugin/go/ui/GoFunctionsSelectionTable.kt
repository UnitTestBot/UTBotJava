package org.utbot.intellij.plugin.go.ui

import com.goide.psi.GoNamedElement
import com.goide.refactor.ui.GoDeclarationInfo
import com.intellij.refactoring.ui.AbstractMemberSelectionTable
import com.intellij.ui.RowIcon
import com.intellij.util.PlatformIcons
import javax.swing.Icon

class GoFunctionsSelectionTable(infos: Set<GoDeclarationInfo>) :
    AbstractMemberSelectionTable<GoNamedElement, GoDeclarationInfo>(infos, null, null) {

    override fun getAbstractColumnValue(info: GoDeclarationInfo): Boolean {
        return info.isToAbstract
    }

    override fun isAbstractColumnEditable(rowIndex: Int): Boolean {
        return myMemberInfoModel.isAbstractEnabled(myMemberInfos[rowIndex] as GoDeclarationInfo)
    }

    override fun getOverrideIcon(memberInfo: GoDeclarationInfo): Icon? = null

    override fun setVisibilityIcon(memberInfo: GoDeclarationInfo, icon_: com.intellij.ui.icons.RowIcon?) {
        val icon = icon_ as RowIcon
        val iconToSet = if (memberInfo.declaration.isPublic) {
            PlatformIcons.PUBLIC_ICON
        } else {
            PlatformIcons.PRIVATE_ICON
        }
        icon.setIcon(iconToSet, VISIBILITY_ICON_POSITION)
    }
}