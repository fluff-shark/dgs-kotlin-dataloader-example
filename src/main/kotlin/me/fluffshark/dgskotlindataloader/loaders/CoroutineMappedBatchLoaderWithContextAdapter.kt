package me.fluffshark.dgskotlindataloader.loaders

internal class CoroutineMappedBatchLoaderWithContextAdapter<K, V>(
  private val loader: CoroutineMappedBatchLoaderWithContext<K, V>,
  private val context: CustomContext,
  maxBatchSize: Int,
) : AbstractCoroutineDataLoader<K, V>(maxBatchSize) {
    override suspend fun doLoad(keys: Set<K>): Map<K, V> {
      return loader.load(keys, context)
    }
}