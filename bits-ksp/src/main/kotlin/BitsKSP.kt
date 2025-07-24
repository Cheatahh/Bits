package cheatahh.bits

import com.google.devtools.ksp.processing.*

/**
 * Provides a [SymbolProcessor] for the KSP (Kotlin Symbol Processing) API.
 *
 * This class implements [SymbolProcessorProvider] and is responsible for creating
 * instances of [BitsKSPProcessor] using the provided [SymbolProcessorEnvironment].
 */
class BitsKSP : SymbolProcessorProvider {
    /**
     * Creates a new instance of [BitsKSPProcessor] with the given [SymbolProcessorEnvironment].
     *
     * @param environment The environment containing utilities and configuration for symbol processing.
     * @return A new [BitsKSPProcessor] instance.
     */
    override fun create(environment: SymbolProcessorEnvironment) =
        BitsKSPProcessor(environment.codeGenerator, environment.logger)
}