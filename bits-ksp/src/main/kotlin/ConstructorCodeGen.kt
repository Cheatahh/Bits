package cheatahh.bits

import java.io.OutputStreamWriter

/**
 * Code generation strategy for constructors.
 */
object ConstructorCodeGen {

    /**
     * Generates parameter code for the given constructor.
     *
     * @param desc The property descriptor.
     * @return The generated code as a String.
     */
    fun parameterCode(desc: PropertyDesc) = desc.properties.joinToString {
        "${it.name.escapeLiteral()}: ${it.targetType.targetTypeQualifiedName.escapeLiteral()}"
    }

    fun valueCode(desc: PropertyDesc) = desc.properties.joinToString(separator = ".or") {
        "(${PropertyCodeGen.Setter.setterValue(desc, it, it.name)})"
    }

    /**
     * Writes the generated code for the constructor to the provided output stream.
     *
     * @param out The OutputStreamWriter to write the code to.
     * @param desc The constructor descriptor.
     */
    fun write(out: OutputStreamWriter, desc: ConstructorDesc) {
        out.write("${desc.receiverVisibility.name.lowercase()} fun ${desc.qualifiedReceiver.escapeLiteral()
            }(${parameterCode(desc.propertyDesc)
            }) = ${desc.receiver.escapeLiteral()}(${desc.propertyDesc.backendName.escapeLiteral()
            } = ${desc.propertyDesc.backendType.toTarget(valueCode(desc.propertyDesc))
            })\n")
    }
}