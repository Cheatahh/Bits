package cheatahh.bits

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Visibility

/**
 * Checks if a [KSAnnotated] is effectively public or internal.
 *
 * @receiver The [KSAnnotated] to check.
 * @return [Visibility] if the element is public or internal, otherwise null.
 */
internal fun KSAnnotated.checkVisibility() =
    when(this) {
        is KSDeclaration -> checkVisibility()
        is KSPropertyAccessor -> checkVisibility()
        else -> null
    }

/**
 * Checks if a [KSDeclaration] is effectively public or internal.
 *
 * @receiver The [KSDeclaration] to check.
 * @return [Visibility] if the element is public or internal, otherwise null.
 */
internal fun KSDeclaration.checkVisibility() =
    when(val it = getVisibility()) {
        Visibility.PUBLIC, Visibility.INTERNAL -> it
        else -> null
    }

/**
 * Checks if a [KSPropertyAccessor] is effectively public or internal.
 *
 * @receiver The [KSPropertyAccessor] to check.
 * @return [Visibility] if the element is public or internal, otherwise null.
 */
internal fun KSPropertyAccessor.checkVisibility(): Visibility? {
    val receiver = receiver.checkVisibility()
    return when {
        receiver == null || modifiers.any { it == Modifier.PRIVATE || it == Modifier.PROTECTED } -> null
        modifiers.any { it == Modifier.INTERNAL } -> Visibility.INTERNAL
        else -> receiver
    }
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

/**
 * Finds a property in the class that is associated with a constructor parameter and is public or internal.
 *
 * This function searches for a constructor with exactly one property parameter (`val` or `var`),
 * matches it to a declared property, and checks if the property is visible (public or internal).
 * Optionally, a specific property name can be provided for filtering.
 *
 * @param propertyName The name of the property to look for, or `null` to match any property.
 * @return The matching Pair([KSPropertyDeclaration], [KSFunctionDeclaration]) if found and visible, otherwise `null`.
 */
fun KSClassDeclaration.getPropertyOfApplicableConstructor(propertyName: String? = null) = getConstructors().firstNotNullOfOrNull { it ->
    when {
        it.checkVisibility() == null -> null
        it.parameters.count(KSValueParameter::isProperty) != 1 -> null
        else -> {
            val parameter = it.parameters.first(KSValueParameter::isProperty)
            val property = getDeclaredProperties().first { it.simpleName == parameter.name }
            if(
                (propertyName == null || property.simpleName.asString() == propertyName)
                && it.parameters.all { it.hasDefault || it.isProperty() }
                && property.checkVisibility() != null
            ) property to it else null
        }
    }
}

/**
 * Checks whether a given [KSValueParameter] declares a property.
 */
fun KSValueParameter.isProperty() = isVal || isVar

/**
 * Returns the fully qualified name of the class, including parent class names if nested.
 *
 * This function recursively traverses parent declarations to build a dot-separated qualifier.
 *
 * @receiver The [KSClassDeclaration] for which to generate the qualifier.
 * @return The simple qualifier as a [String].
 */
fun KSClassDeclaration.getSimpleQualifier(): String {
    return when(val parent = parentDeclaration as? KSClassDeclaration) {
        null -> simpleName.asString()
        else -> "${parent.getSimpleQualifier()}.${simpleName.asString()}"
    }
}

/**
 * Checks whether a given [KSClassDeclaration] is not nested within another class or qualifies as an inner class.
 */
tailrec fun KSClassDeclaration.isNotNestedOrInner(): Boolean {
    val parent = parentDeclaration as? KSClassDeclaration
    return when {
        parent == null -> true
        modifiers.none { it == Modifier.INNER } -> false
        else -> parent.isNotNestedOrInner()
    }
}

private val hardKeywords = setOf(
    "package", "as", "typealias", "class", "this", "super", "val", "var", "fun",
    "for", "null", "true", "false", "is", "in", "throw", "return", "break", "continue",
    "object", "if", "try", "else", "while", "do", "when", "interface"
)
private val isValidIdentifier = Regex("^[a-zA-Z][a-zA-Z0-9_]*$")
/**
 * Escapes a literal string if necessary.
 * */
fun String.escapeLiteral() = split('.').joinToString(separator = ".") {
    if(it.matches(isValidIdentifier) && it !in hardKeywords) it else "`$it`"
}