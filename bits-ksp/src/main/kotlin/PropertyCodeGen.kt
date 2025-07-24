package cheatahh.bits

import java.io.OutputStreamWriter

/**
 * Sealed class representing code generation strategies for bit properties.
 */
sealed class PropertyCodeGen {

    protected fun getBitMask(count: Int) = "1".repeat(count)
    protected abstract val propertyKeyword: String

    /**
     * Generates accessor code for the given property.
     *
     * @param desc The property descriptor.
     * @param property The bit property descriptor.
     * @return The generated code as a String.
     */
    abstract fun code(desc: PropertyDesc, property: BitPropertyDesc): String

    /**
     * Writes the generated code for the property to the provided output stream.
     *
     * @param out The OutputStreamWriter to write the code to.
     * @param desc The property descriptor.
     * @param property The bit property descriptor.
     */
    fun write(out: OutputStreamWriter, desc: PropertyDesc, property: BitPropertyDesc) {
        out.write("$propertyKeyword ${if(desc.receiver != null) "${desc.receiver}." else ""}${property.name} ${code(desc, property)}\n")
    }

    /**
     * Code generation strategy for property getters.
     */
    object Getter : PropertyCodeGen() {

        override val propertyKeyword get() = "val"

        /**
         * Generates the getter code for the given property.
         *
         * @param desc The property descriptor containing backend and property information.
         * @param property The bit property descriptor.
         * @return The generated getter code as a String.
         */
        override fun code(desc: PropertyDesc, property: BitPropertyDesc) = "get() = ${property.targetType.toTarget("${desc.backendType.toIntermediate(desc.backendName)
        }.${desc.backendType.intermediateShiftPrefix
        }shr(${property.getterBitOffset
        }).and(0b${getBitMask(property.getterBitCount)
        }${desc.backendType.intermediateLiteralExtension
        })")}"
    }
    object Setter : PropertyCodeGen() {

        override val propertyKeyword get() = "var"

        /**
         * Generates the setter code for the given property.
         *
         * @param desc The property descriptor containing backend and property information.
         * @param property The bit property descriptor.
         * @return The generated setter code as a String.
         */
        override fun code(desc: PropertyDesc, property: BitPropertyDesc) = "get(): ${property.targetType
            } = throw UnsupportedOperationException(\"This property only supports writing.\"); ${setterCode(desc, property)
            }"
        internal fun setterCode(desc: PropertyDesc, property: BitPropertyDesc) = "set(${getFrontendName(desc)}) { ${desc.backendName
            } = ${desc.backendType.toTarget("${desc.backendType.toIntermediate(desc.backendName)
            }.and(0b${getBitMask(property.setterBitCount)
            }${desc.backendType.intermediateLiteralExtension
            }.shl(${property.setterBitOffset
            }).inv()).or(${desc.backendType.toIntermediate("${property.targetType.toIntermediate(getFrontendName(desc))
            }.and(0b${getBitMask(property.setterBitCount)
            }${property.targetType.intermediateLiteralExtension
            })")}.shl(${property.setterBitOffset
            }))")
            } }"
        private fun getFrontendName(desc: PropertyDesc) = if(desc.backendName == "value") "value_" else "value"
    }

    /**
     * Code generation strategy for properties supporting both getter and setter.
     */
    object All : PropertyCodeGen() {

        override val propertyKeyword = "var"

        /**
         * Generates both getter and setter code for the property.
         *
         * @param desc The property descriptor.
         * @param property The bit property descriptor.
         * @return The combined getter and setter code as a String.
         */
        override fun code(desc: PropertyDesc, property: BitPropertyDesc) = "${Getter.code(desc, property)}; ${Setter.setterCode(desc, property)}"
    }
}