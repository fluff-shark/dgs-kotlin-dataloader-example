package me.fluffshark.dgskotlindataloader.loaders

/**
 * Like a [org.dataloader.MappedBatchLoader], but with coroutines.
 *
 * Implementations _must_ be:
 *
 *   1. Annotated wiht @KotlinDataLoader
 *   2. Bound to the singleton scope.
 */
interface CoroutineMappedBatchLoader<K, V> {
  suspend fun load(keys: Set<K>): Map<K, V>
}