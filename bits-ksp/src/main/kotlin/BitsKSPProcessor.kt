package cheatahh.bits

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*

/**
 * Symbol processor for handling BitField annotations in Kotlin Symbol Processing (KSP).
 *
 * This processor groups annotated symbols by package and delegates processing to
 * [BitsKSPPackageProcessor], which generates code for bitfield properties.
 *
 * @property gen The code generator used to create new files.
 * @property logger The logger for reporting messages and errors.
 */
class BitsKSPProcessor(private val gen: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    private var processingRound = 0

    /**
     * Processes symbols with the [BitField] annotation.
     *
     * Groups symbols by their package and invokes the package processor for each group.
     *
     * @param resolver The resolver for symbol lookup and type resolution.
     * @return A list of deferred annotated nodes.
     */
    override fun process(resolver: Resolver) = buildList defer@ {
        val lookup = DataType.Lookup(resolver)
        resolver.getSymbolsWithAnnotation(BitField::class.java.name).groupBy {
            (getFile(it) as KSFile).packageName.asString()
        }.forEach { (packageName, symbols) ->
            BitsKSPPackageProcessor(packageName, lookup, logger).process(symbols, gen, this@defer, processingRound)
        }
        processingRound++
    }
}