package cheatahh.bits

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Processes all bitfield-related properties and accessors within a given package for code generation.
 *
 * This class collects annotated properties and their accessors, checks their visibility,
 * and generates a single adapter file containing the generated bitfield property code.
 *
 * @property packageName The name of the package being processed.
 * @property lookup The data type lookup utility for resolving types.
 * @property logger The logger for reporting messages and errors.
 */
class BitsKSPPackageProcessor(private val packageName: String, private val lookup: DataType.Lookup, private val logger: KSPLogger) {
    /**
     * Processes the provided symbols, grouping them by property, checking accessibility,
     * and generating the adapter file with bitfield property code.
     *
     * @param symbols The list of annotated symbols to process.
     * @param gen The code generator used to create new files.
     * @param unresolved A list of annotated nodes to move to the next processing round.
     * @param processingRound The current round of annotation processing.
     */
    fun process(symbols: List<KSAnnotated>, gen: CodeGenerator, unresolved: MutableList<KSAnnotated>, processingRound: Int) {
        logger.info("Processing package '$packageName' with ${symbols.size} symbol(s).")
        // group annotated nodes by property
        val properties = symbols.filterIsInstance<KSPropertyAccessor>().groupByTo(mutableMapOf()) { it.receiver }
        symbols.filterIsInstance<KSPropertyDeclaration>().forEach { properties.putIfAbsent(it, mutableListOf()) }
        // generated code is within single file
        gen.createNewFile(Dependencies(true), packageName, "BitsAdapter${processingRound}", "kt").writer().use { stream ->
            stream.write("""@file:Suppress(
                |"RemoveRedundantCallsOfConversionMethods",
                |"PackageDirectoryMismatch",
                |"RemoveRedundantQualifierName",
                |"RedundantVisibilityModifier",
                |"FunctionName",
                |"LocalVariableName",
                |"RemoveRedundantBackticks"
                |)
                |package $packageName
                |""".trimMargin())
            val processor = BitsKSPPropertyProcessor(lookup, logger)
            for((property, accessors) in properties)
                processor.process(property, accessors, unresolved).forEach { it.writeTo(stream) }
        }
    }
}