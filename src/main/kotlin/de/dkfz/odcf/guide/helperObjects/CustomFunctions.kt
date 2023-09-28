package de.dkfz.odcf.guide.helperObjects

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.reflect.full.superclasses

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

/**
 * Returns a list containing only the non-null or non-blank and distinct results
 * of applying the given [transform] function to each element in the original collection.
 */
inline fun <T, reified R> Iterable<T>.mapDistinctAndNotNullOrBlank(transform: (T) -> R?): List<R> {
    if (R::class.superclasses.contains(CharSequence::class)) {
        return mapNotNull { value -> transform(value).takeIf { it is CharSequence && it.isNotBlank() } }.distinct()
    }
    return mapNotNull { transform(it) }.distinct()
}
