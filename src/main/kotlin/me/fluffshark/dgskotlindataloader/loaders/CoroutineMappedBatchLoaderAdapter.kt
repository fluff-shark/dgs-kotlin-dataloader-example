package me.fluffshark.dgskotlindataloader.loaders

import me.fluffshark.dgskotlindataloader.loaders.AbstractCoroutineDataLoader

internal class CoroutineMappedBatchLoaderAdapter<K, V>(
  private val loader: CoroutineMappedBatchLoader<K, V>,
  maxBatchSize: Int,
): AbstractCoroutineDataLoader<K, V>(maxBatchSize) {
  override suspend fun doLoad(keys: Set<K>): Map<K, V> {
    return loader.load(keys)
  }
}