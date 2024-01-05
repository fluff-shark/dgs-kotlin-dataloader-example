package me.fluffshark.dgskotlindataloader.loaders

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import me.fluffshark.dgskotlindataloader.loaders.CoroutineDataLoader

class CoroutineDataLoaderRegistry(
  private val provider: CoroutineDataLoaderProvider,
) {
  private val loaderMap = ConcurrentHashMap<KClass<*>, CoroutineDataLoader<*, *>>()

  fun <K, V, T: CoroutineMappedBatchLoader<K, V>> getDataLoader(
    c: KClass<T>,
  ): CoroutineDataLoader<K, V> {
    // cast is safe because the only way stuff gets put in the map is through these "get" methods,
    // which are protected by their own generics.
    @Suppress("UNCHECKED_CAST")
    return loaderMap.getOrPut(c) {
      val instanceAndMetadata = provider.getInstanceAndMetadata(c)
      CoroutineMappedBatchLoaderAdapter(instanceAndMetadata.instance, instanceAndMetadata.maxBatchSize)
    } as CoroutineDataLoader<K, V>
  }

  fun <K, V, T : CoroutineMappedBatchLoaderWithContext<K, V>> getDataLoaderWithContext(
    c: KClass<T>,
    context: CustomContext
  ): CoroutineDataLoader<K, V> {
    // cast is safe because the only way stuff gets put in the map is through these "get" methods,
    // which are protected by their own generics.
    @Suppress("UNCHECKED_CAST")
    return loaderMap.getOrPut(c) {
      val instanceAndMetadata = provider.getInstanceAndMetadata(c)
      CoroutineMappedBatchLoaderWithContextAdapter(
        instanceAndMetadata.instance,
        context,
        instanceAndMetadata.maxBatchSize
      )
    } as CoroutineDataLoader<K, V>
  }
}