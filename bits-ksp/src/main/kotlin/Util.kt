package cheatahh.bits

import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSModifierListOwner
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * Checks if a [KSModifierListOwner] is effectively public or internal.
 *
 * This function recursively checks the modifiers of the element and its parents to determine
 * if the element is accessible (public or internal). It also handles property accessors and overrides.
 *
 * @receiver The [KSModifierListOwner] to check.
 * @return `true` if the element is public or internal, otherwise `false`.
 */
internal fun KSModifierListOwner.isEffectivelyPublicOrInternal(): Boolean {
    when {
        Modifier.PRIVATE in modifiers -> return false
        Modifier.PROTECTED in modifiers -> return false
        Modifier.INTERNAL in modifiers -> return true
        Modifier.PUBLIC in modifiers -> return true
    }
    if (this is KSPropertyAccessor) return receiver.isEffectivelyPublicOrInternal()
    if (this is KSPropertyDeclaration && Modifier.OVERRIDE in modifiers) {
        val override = findOverridee()
        if (override != null) return override.isEffectivelyPublicOrInternal()
    }
    var parent = parent
    while(parent != null && parent.parent !is KSModifierListOwner) parent = parent.parent
    return if (parent != null) (parent as KSModifierListOwner).isEffectivelyPublicOrInternal() else true
}

/**
 * Recursively finds the [KSFile] node for a given [KSNode].
 *
 * @param it The [KSNode] to start from.
 * @return The [KSFile] node or the topmost node if no file is found.
 */
internal tailrec fun getFile(it: KSNode): KSNode = when {
    it.parent == null || it is KSFile -> it
    else -> getFile(it.parent!!)
}