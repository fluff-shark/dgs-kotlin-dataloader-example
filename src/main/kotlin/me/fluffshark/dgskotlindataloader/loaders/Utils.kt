package me.fluffshark.dgskotlindataloader.loaders

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

internal suspend fun <T, V> Iterable<T>.pmap(f: suspend (T) -> V): List<V> = coroutineScope {
  map { async { f(it) } }.awaitAll()
}