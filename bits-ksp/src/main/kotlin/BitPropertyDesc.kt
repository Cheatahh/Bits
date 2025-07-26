package cheatahh.bits

import com.google.devtools.ksp.symbol.Visibility

/**
 * Describes a bit-property for code generation, including getter and setter information.
 *
 * @property name The name of the property.
 * @property getterBitCount The number of bits used by the getter.
 * @property getterBitOffset The bit offset for the getter.
 * @property setterBitCount The number of bits used by the setter.
 * @property setterBitOffset The bit offset for the setter.
 * @property targetType The data type of the property's value.
 * @property codegen The code generation strategy for this property.
 */
data class BitPropertyDesc (
    val name: String,
    val getterBitCount: Int,
    var getterBitOffset: Int,
    val getterVisibility: Visibility,
    val setterBitCount: Int,
    var setterBitOffset: Int,
    val setterVisibility: Visibility,
    val targetType: DataType,
    val codegen: PropertyCodeGen
)