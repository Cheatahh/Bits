package cheatahh.bits

import java.io.OutputStreamWriter

/**
 * Describes a property container for code generation.
 *
 * @property receiver The receiver type name, or null if not applicable.
 * @property backendName The name of the backend field or property.
 * @property backendType The data type of the backend.
 * @property properties The list of bit-property descriptors associated with this property.
 */
data class PropertyDesc (
    val receiver: String?,
    val backendName: String,
    val backendType: DataType,
    val properties: List<BitPropertyDesc>
) : WritableDesc {
    /**
     * Writes all properties using their code generation strategy to the provided output stream.
     *
     * @param out The OutputStreamWriter to write the generated code to.
     */
    override fun writeTo(out: OutputStreamWriter) {
        for(property in properties) property.codegen.write(out, this, property)
    }
}

