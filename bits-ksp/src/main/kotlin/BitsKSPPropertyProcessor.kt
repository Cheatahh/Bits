package cheatahh.bits

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSType
import kotlin.collections.iterator
import kotlin.collections.plusAssign
import kotlin.sequences.forEach

/**
 * Processes bitfield property annotations and generates property descriptors for code generation.
 *
 * This class analyzes annotated properties and their accessors, synchronizes getter and setter information,
 * checks for duplicates, assigns offsets, and creates a list of bit property descriptors for code generation.
 *
 * @property lookup The data type lookup utility for resolving types.
 * @property logger The logger for reporting messages and errors.
 */
class BitsKSPPropertyProcessor(private val lookup: DataType.Lookup, private val logger: KSPLogger) {
    /**
     * Processes a property and its accessors to generate a [PropertyDesc] for code generation.
     *
     * @param property The property declaration to process.
     * @param accessors The list of property accessors (getters/setters).
     * @return A [PropertyDesc] containing bit-property descriptors, or null if there are errors.
     */
    fun process(property: KSPropertyDeclaration, accessors: List<KSPropertyAccessor>): PropertyDesc? {
        val receiver = (property.extensionReceiver?.resolve()?.declaration ?: property.parentDeclaration)?.qualifiedName?.asString()
        val backendName = property.simpleName.asString()
        val backendTypeRef = property.type.resolve()
        val backendType = lookup[backendTypeRef]
        if(backendType == null) {
            logger.error("Type ${backendTypeRef.declaration.simpleName.asString()} not supported.", property)
            return null
        }
        val getterProperties = mutableSetOf<BitProperty>()
        val setterProperties = mutableSetOf<BitProperty>()
        // fill virtual getters & setters from annotation information
        property.getBits(backendType, lookup).forEach {
            getterProperties += it
            if(property.isMutable) setterProperties += it
        }
        accessors.forEach {
            (if(it is KSPropertyGetter) getterProperties else setterProperties) += it.getBits(backendType, lookup)
        }
        // remove implicitly defined values (by context)
        removeImplicitlyDefinedWildcards(getterProperties)
        removeImplicitlyDefinedWildcards(setterProperties)
        // if property contains duplicate virtual accessors -> error
        if(checkForDuplicates(setterProperties) || checkForDuplicates(getterProperties)) return null

        val commonLabels = getterProperties.map(BitProperty::name).intersect(setterProperties.map(BitProperty::name))
        // make sure getter & setter have same offset, type & size
        syncDefinedWildcards(commonLabels, getterProperties, setterProperties)
        syncDefinedWildcards(commonLabels, setterProperties, getterProperties)
        // place undefined offsets to fit into structure
        defineWildcards(backendType, getterProperties)
        defineWildcards(backendType, setterProperties)

        val properties = mutableListOf<BitPropertyDesc>()
        // virtual properties with getter & setter
        commonLabels.forEach { label ->
            val getter = getterProperties.first { it.name == label }
            val setter = setterProperties.first { it.name == label }
            if(getter.targetType != setter.targetType) {
                logger.error("Bit property '$label' getter and setter must match data types.", property)
                return null
            }
            if(getter.offset != setter.offset || getter.count != setter.count) {
                logger.warn("Bit property '$label' getter and setter should match offset and size.", property)
            }
            properties += BitPropertyDesc(label,
                getter.count, getter.offset,
                setter.count, setter.offset,
                getter.targetType,
                PropertyCodeGen.All
            )
        }
        // virtual getter-only properties
        getterProperties.forEach { if(it.name !in commonLabels) properties += it.toDesc(PropertyCodeGen.Getter) }
        // virtual setter-only properties
        setterProperties.forEach { if(it.name !in commonLabels) properties += it.toDesc(PropertyCodeGen.Setter) }
        logger.info("Property ${if(receiver != null) "$receiver." else ""}$backendName:$backendType with \n${
            properties.joinToString(separator = "\n") { "\t${it.name}:${it.targetType} ${if(it.codegen != PropertyCodeGen.Setter) "GET[${it.getterBitOffset}:${it.getterBitCount}]" else ""}${if(it.codegen != PropertyCodeGen.Getter) "SET[${it.setterBitOffset}:${it.setterBitCount}]" else ""}" }
        }", property)
        return PropertyDesc (
            receiver,
            backendName,
            backendType,
            properties
        )
    }

    /**
     * Removes implicitly defined wildcard bit properties from the set.
     *
     * @param set The set of bit properties to filter.
     */
    private fun removeImplicitlyDefinedWildcards(set: MutableSet<BitProperty>) {
        val defined = set.filter(BitProperty::isOffsetDefined)
        val iterator = set.iterator()
        for(desc in iterator) {
            if(desc.isOffsetDefined) continue
            if(defined.any {
                it.name == desc.name && it.count == desc.count && it.targetType == desc.targetType
            }) iterator.remove()
        }
    }

    /**
     * Synchronizes offsets for wildcard bit properties between getter and setter sets.
     *
     * @param commonLabels The set of property names present in both getter and setter.
     * @param from The source set to copy offsets from.
     * @param to The target set to update offsets in.
     */
    private fun syncDefinedWildcards(commonLabels: Set<String>, from: Set<BitProperty>, to: Set<BitProperty>) {
        for(desc in to) {
            if(desc.name !in commonLabels || desc.isOffsetDefined) continue
            val resolved = from.firstOrNull { it.name == desc.name && it.isOffsetDefined } ?: continue
            desc.offset = resolved.offset
        }
    }

    /**
     * Checks for duplicate bit property names in the set and logs errors.
     *
     * @param set The set of bit properties to check.
     * @return `true` if duplicates are found, otherwise `false`.
     */
    private fun checkForDuplicates(set: Set<BitProperty>): Boolean {
        return set.groupBy { it.name }.filter { it.value.count() > 1 }.onEach { list -> list.value.forEach {
            logger.error("Duplicate bit property ${it.name} found.", it.node)
        } }.isNotEmpty()
    }

    /**
     * Assigns offsets to wildcard bit properties to fit them into the backend type's bit range.
     *
     * @param backendType The backend data type.
     * @param set The set of bit properties to assign offsets to.
     *
     * @return `true` if successful, otherwise `false`.
     */
    private fun defineWildcards(backendType: DataType, set: MutableSet<BitProperty>): Boolean {
        val ranges = mutableListOf(0 ..< backendType.bitCount)
        for(desc in set) {
            if(!desc.isOffsetDefined) continue
            ranges.removeAll { it.first >= desc.offset && it.last < desc.offset + desc.count }
            var rangeWithOffset = ranges.indexOfFirst { desc.offset in it }
            if(rangeWithOffset != -1) {
                val newRange = ranges[rangeWithOffset].first ..< desc.offset
                if(!newRange.isEmpty()) ranges[rangeWithOffset] = newRange else if(ranges.size != 1) ranges.removeAt(rangeWithOffset)
            }
            rangeWithOffset = ranges.indexOfFirst { desc.offset + desc.count - 1 in it }
            if(rangeWithOffset != -1) {
                val newRange = desc.offset + desc.count ..ranges[rangeWithOffset].last
                if(!newRange.isEmpty()) ranges[rangeWithOffset] = newRange else ranges.removeAt(rangeWithOffset)
            }
        }
        for(desc in set) {
            if(desc.offset != -1) continue
            val matchingRangeIndex = ranges.indexOfFirst { it.last - it.first + 1 >= desc.count }
            if(matchingRangeIndex == -1) {
                logger.error("Not enough space to fit bit property ${desc.name}.", desc.node)
                return false
            }
            desc.offset = ranges[matchingRangeIndex].first
            val newRange = desc.offset + desc.count ..ranges[matchingRangeIndex].last
            if(!newRange.isEmpty()) ranges[matchingRangeIndex] = newRange else ranges.removeAt(matchingRangeIndex)
        }
        return true
    }

    /**
     * Converts a [KSAnnotation] to a [BitProperty] for code generation.
     *
     * @param backendType The backend data type.
     * @param lookup The data type lookup utility.
     * @return A [BitProperty] instance, or null if validation fails.
     */
    private fun KSAnnotation.toBits(backendType: DataType, lookup: DataType.Lookup) = BitProperty (
        arguments.first { it.name?.asString() == "name" }.value as String,
        (arguments.first { it.name?.asString() == "count" }.value as UByte).toInt(),
        (arguments.first { it.name?.asString() == "offset" }.value as Byte).toInt(),
        lookup[arguments.first { it.name?.asString() == "type" }.value as KSType, backendType].let nullcheck@ { type ->
            if(type == null) {
                logger.error("Type ${
                    (arguments.first { it.name?.asString() == "type" }.value as KSType).declaration.simpleName.asString()
                } not supported.", this@toBits)
                return@toBits null
            }
            type
        },
        this
    ).takeIf {
        when {
            it.count > it.targetType.bitCount -> {
                logger.error("Bit property ${it.name} may not be larger than its data type.", this)
                false
            }
            !(it.offset == -1 || it.offset + it.count <= backendType.bitCount) -> {
                logger.error("Bit property layout of ${it.name} may not exceed its backend data type.", this)
                false
            }
            else -> true
        }
    }

    /**
     * Extracts all [BitProperty]s from the annotations of a [KSAnnotated] element.
     *
     * @param backendType The backend data type.
     * @param lookup The data type lookup utility.
     * @return A list of [BitProperty]s found in the annotations.
     */
    private fun KSAnnotated.getBits(backendType: DataType, lookup: DataType.Lookup) = annotations.mapNotNull {
        it.takeIf {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == BitField::class.java.name
        }?.toBits(backendType, lookup)
    }
}