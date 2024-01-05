package me.fluffshark.dgskotlindataloader.loaders

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

interface CoroutineDataLoader<K, V> {
  /** Load the data for a single key. */
  suspend fun load(key: K): V?
  /** Load the data for many keys at once. */
  suspend fun loadMany(keys: List<K>): List<V?> = keys.pmap { load(it) }
}
