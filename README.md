# Bits | BitField Annotation Processor

This repository provides a Kotlin annotation, `@BitField`, along with a [Kotlin Symbol Processing (KSP)](https://kotlinlang.org/docs/ksp-overview.html) 
processor which generates virtual accessors for working with bit-level fields within structured data.

## Overview
The `@BitField` annotation enables declarative definition of bitfield mappings in Kotlin code. 
It is particularly useful when working with compact binary representations, such as protocol headers or memory-mapped data structures.

This project includes:
- A Kotlin annotation: `@BitField`, repeatable and applicable to `AnnotationTarget.PROPERTY` & accessors.
- A KSP processor which generates code for virtual getters and setters based on your annotations

## Annotation Definition
```kotlin
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class BitField (
    val name: String, // The name of the bitfield to generate.
    val count: UByte, // Number of bits the field spans.
    val offset: Byte = -1, // Bit offset within the container. Defaults to -1 (= auto-calculated).
    val type: KClass<*> = Any::class // Kotlin class type for this field. Defaults to Any (= auto-calculated)
)
```

## Example Usage
```kotlin
@get:BitField(name = "flagA", count = 1u, type = Boolean::class)
@BitField(name = "valueB", count = 4u, offset = 10)
var myField = 0b1 // Results in layout [0-1:flagA] [1-10] [10-14:valueB] [14-32]
println(flagA) // true
valueB = 42 // 0b101010
println(valueB) // 10, as only 4 bits are stored
println(myField) // 10241
```
```kotlin
@JvmInline
value class Token (
  @BitField("type", 10u)
  @BitField("offset", 32u)
  @BitField("length", 22u)
  val data: ULong // Results in layout [0-10:type] [10-42:offset] [42-64:length]
)
val t = Token(5309480349674895u)
println(t.type) // 1423
println(t.offset) // 1013877707
println(t.length) // 1207
```
After processing, the KSP processor will generate virtual accessors to manipulate the appropriate bits of the annotated property.

## How It Works
- You annotate properties with @BitField to define bit layouts.
- The KSP processor reads the annotations at compile-time and generates type-safe accessor methods.
- These accessors allow you to interact with bit-level data using regular Kotlin properties.

Happy coding!
