package me.fluffshark.dgskotlindataloader.loaders

/**
 * Like a [org.dataloader.MappedBatchLoaderWithContext], but using coroutines and more tightly
 * coupled to the DGS custom context for convenience.
 *
 * Implementations _must_ be:
 *
 *   1. Annotated wiht @KotlinDataLoader
 *   2. Bound to the singleton scope.
 */
interface CoroutineMappedBatchLoaderWithContext<K, V> {
  suspend fun load(keys: Set<K>, context: CustomContext): Map<K, V>
}
