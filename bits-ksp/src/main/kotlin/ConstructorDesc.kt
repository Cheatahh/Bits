package cheatahh.bits

import com.google.devtools.ksp.symbol.Visibility
import java.io.OutputStreamWriter

/**
 * Describes a constructor for code generation.
 *
 * @property receiver The receiver type name.
 * @property receiverVisibility The receiver visibility.
 * @property propertyDesc The descriptor of the declared property within the constructor.
 */
data class ConstructorDesc (
    val receiver: String,
    val qualifiedReceiver: String,
    val receiverVisibility: Visibility,
    val propertyDesc: PropertyDesc
) : WritableDesc {
    /**
     * Writes the constructor using the [ConstructorCodeGen] strategy to the provided output stream.
     *
     * @param out The OutputStreamWriter to write the generated code to.
     */
    override fun writeTo(out: OutputStreamWriter) {
        ConstructorCodeGen.write(out, this)
    }
}