package de.dkfz.odcf.guide.helperObjects

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

fun String.toBool(): Boolean {
    return when (this.lowercase()) {
        "1", "true", "t", "yes", "y", "on" -> true
        // "0", "false", "f", "no", "" -> false // not needed
        else -> false
    }
}

fun String.toKebabCase() = replace(humps, "-").lowercase()
val humps = "(?<=.)(?=\\p{Upper})".toRegex()

suspend fun <A, B> Iterable<A>.mapParallel(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

suspend fun <A, B> Iterable<A>.setParallel(f: suspend (A) -> B): Set<B> = coroutineScope {
    mapParallel { f(it) }.toSet()
}
