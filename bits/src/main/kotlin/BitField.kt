package cheatahh.bits

import kotlin.reflect.KClass

/**
 * Annotation to specify bit field properties for a property.
 *
 * This annotation can be applied to properties, their getters, or setters to generate virtual accessors.
 *
 * @property name The name of the bitfield to generate.
 * @property count The number of bits used for this field.
 * @property offset The bit offset within the containing structure. Defaults to -1 if unspecified.
 * @property type The Kotlin class type associated with this bit field. Defaults to [Any] for backend-type reference.
 *
 * @constructor Creates a new [BitField] annotation instance.
 */
@Target (
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class BitField (
    val name: String,
    val count: UByte,
    val offset: Byte = -1,
    val type: KClass<*> = Any::class
)