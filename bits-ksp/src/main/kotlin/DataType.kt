package cheatahh.bits

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

/**
 * Represents a data type abstraction for code generation and symbol processing.
 */
sealed interface DataType {

    /**
     * Converts the given code to its intermediate type representation.
     *
     * @param code The code to convert.
     * @return The intermediate type representation as a code string.
     */
    fun toIntermediate(code: String): String

    /**
     * Converts the given code to its target type representation.
     *
     * @param code The code to convert.
     * @return The target type representation as a code string.
     */
    fun toTarget(code: String): String

    /**
     * The extension used for literals of the intermediate data type.
     */
    val intermediateLiteralExtension: String

    /**
     * The prefix used for shift operations of the intermediate data type.
     */
    val intermediateShiftPrefix: String

    /**
     * The number of bits used by this data type.
     */
    val bitCount: Int

    /**
    * The qualified name of the target data type.
    * */
    val targetTypeQualifiedName: String

    /**
     * Enum representing primitive data types and their conversion logic.
     *
     * @property toIntermediate Function to convert code to intermediate representation.
     * @property toTarget Function to convert code to type representation.
     * @property intermediateLiteralExtension Extension for literals.
     * @property intermediateShiftPrefix Prefix for shift operations.
     * @property bitCount Number of bits for the type.
     */
    enum class Primitive(val toIntermediate: (code: String) -> String, val toTarget: (code: String) -> String, override val intermediateLiteralExtension: String, override val intermediateShiftPrefix: String, override val bitCount: Int): DataType {
        Byte(toIntermediate = { "($it).toInt()" }, toTarget = { "($it).toByte()" }, intermediateLiteralExtension = "", intermediateShiftPrefix = "u", bitCount = 8),
        UByte(toIntermediate = { "($it).toUInt()" }, toTarget = { "($it).toUByte()" }, intermediateLiteralExtension = "u", intermediateShiftPrefix = "", bitCount = 8),
        Short(toIntermediate = { "($it).toInt()" }, toTarget = { "($it).toShort()" }, intermediateLiteralExtension = "", intermediateShiftPrefix = "u", bitCount = 16),
        UShort(toIntermediate = { "($it).toUInt()" }, toTarget = { "($it).toUShort()" }, intermediateLiteralExtension = "u", intermediateShiftPrefix = "", bitCount = 16),
        Int(toIntermediate = { "($it).toInt()" }, toTarget = { "($it).toInt()" }, intermediateLiteralExtension = "", intermediateShiftPrefix = "u", bitCount = 32),
        UInt(toIntermediate = { "($it).toUInt()" }, toTarget = { "($it).toUInt()" }, intermediateLiteralExtension = "u", intermediateShiftPrefix = "", bitCount = 32),
        Long(toIntermediate = { "($it).toLong()" }, toTarget = { "($it).toLong()" }, intermediateLiteralExtension = "L", intermediateShiftPrefix = "u", bitCount = 64),
        ULong(toIntermediate = { "($it).toULong()" }, toTarget = { "($it).toULong()" }, intermediateLiteralExtension = "uL", intermediateShiftPrefix = "", bitCount = 64),
        Boolean(toIntermediate = { "(if($it) 1 else 0).toInt()" }, toTarget = { "(($it).toInt() != 0)" }, intermediateLiteralExtension = "", intermediateShiftPrefix = "u", bitCount = 1);
        override fun toIntermediate(code: String) = toIntermediate.invoke(code)
        override fun toTarget(code: String) = toTarget.invoke(code)
        override val targetTypeQualifiedName get() = "kotlin.$name"
    }

    /**
     * Wrapper class for custom data types, delegating most behavior to the underlying property type.
     *
     * @property targetTypeQualifiedName The name of the wrapper class.
     * @property propertyName The name of the property being wrapped.
     * @property propertyType The underlying data type of the property.
     */
    class Wrapper(override val targetTypeQualifiedName: String, private val propertyName: String, private val propertyType: DataType) : DataType by propertyType {
        override fun toTarget(code: String) = "($targetTypeQualifiedName(${propertyName.escapeLiteral()} = ${propertyType.toTarget(code)}))"
        override fun toIntermediate(code: String) = "(${propertyType.toIntermediate("($code).${propertyName.escapeLiteral()}")})"
    }

    /**
     * Lookup class for resolving KSType to DataType, including support for primitive and wrapper types.
     *
     * @constructor Initializes the lookup with a KSP resolver.
     * @param resolver The KSP resolver used for symbol resolution.
     */
    class Lookup(resolver: Resolver) {

        private val anyType = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString("kotlin.Any")
        )!!.asStarProjectedType()

        private val ksTypeToDataType: MutableMap<KSType, DataType> = Primitive.entries.associateByTo(mutableMapOf()) {
            resolver.getClassDeclarationByName(
                resolver.getKSNameFromString("kotlin.${it.name}")
            )!!.asStarProjectedType()
        }

        /**
         * Gets the DataType for the given KSType, using the backend type if the type is kotlin.Any.
         *
         * @param type The KSType to resolve.
         * @param backendType The backend DataType to use if type is kotlin.Any.
         * @return The resolved DataType.
         */
        operator fun get(type: KSType, backendType: DataType? = null) = if(type == anyType) backendType else ksTypeToDataType.getOrPut(type) { createWrapperOrNull(type) ?: return@get null }

        private fun createWrapperOrNull(type: KSType): Wrapper? {
            val classDeclaration = type.declaration as? KSClassDeclaration ?: return null
            val (propertyDeclaration, _) = classDeclaration.getPropertyOfApplicableConstructor() ?: return null
            val propertyType = get(propertyDeclaration.type.resolve()) ?: return null
            return Wrapper(
                classDeclaration.qualifiedName!!.asString().escapeLiteral(),
                propertyDeclaration.simpleName.asString(),
                propertyType
            )
        }
    }
}