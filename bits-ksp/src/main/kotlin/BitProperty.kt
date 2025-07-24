package cheatahh.bits

import com.google.devtools.ksp.symbol.KSNode

/**
 * Represents a virtual bit-property.
 *
 * @property name The name of the property.
 * @property count The number of bits used by the property.
 * @property offset The bit offset for the property.
 * @property targetType The data type of the property's value.
 * @property node The original source node.
 */
data class BitProperty (
    val name: String,
    val count: Int,
    var offset: Int,
    val targetType: DataType,
    val node: KSNode
) {

    internal val isOffsetDefined get() = offset != -1

    /**
     * Converts this BitProperty to a BitPropertyDesc for code generation.
     *
     * @param codegen The code generation strategy to use.
     * @return A BitPropertyDesc instance representing this property.
     */
    fun toDesc(codegen: PropertyCodeGen) = BitPropertyDesc(name,
        count, offset,
        count, offset,
        targetType, codegen
    )
}