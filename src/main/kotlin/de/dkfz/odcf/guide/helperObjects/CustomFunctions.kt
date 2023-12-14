package de.dkfz.odcf.guide.helperObjects

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.URLEncoder
import kotlin.reflect.full.superclasses

fun String.toBool(): Boolean {
    return when (this.lowercase()) {
        "1", "true", "t", "yes", "y", "on" -> true
        // "0", "false", "f", "no", "" -> false // not needed
        else -> false
    }
}

fun String.encodeUtf8(): String = URLEncoder.encode(this, "utf-8")

fun String.toKebabCase() = replace(humps, "-").replace("_", "-").lowercase()
// Checks for a position in a String where there's a character (not a hyphen or underscore) followed by an uppercase letter,
// with no uppercase letters or underscores immediately before it.
// unfortunately does not work with the edge case "KebaBCAse"
val humps = "(?<=.)(?<![_-])(?=\\p{Upper})(?<![_\\p{Upper}])".toRegex()

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

inline fun <reified T : Enum<T>> valueOf(type: String): T? {
    return try {
        java.lang.Enum.valueOf(T::class.java, type)
    } catch (e: IllegalArgumentException) {
        null
    }
}

inline fun <reified T : Enum<T>> valueOf(type: String, default: T): T = valueOf<T>(type) ?: default
