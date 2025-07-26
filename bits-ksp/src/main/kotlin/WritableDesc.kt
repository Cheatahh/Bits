package cheatahh.bits

import java.io.OutputStreamWriter

/**
 * Object which can be written to an [java.io.OutputStreamWriter].
 * */
sealed interface WritableDesc {
    fun writeTo(out: OutputStreamWriter)
}