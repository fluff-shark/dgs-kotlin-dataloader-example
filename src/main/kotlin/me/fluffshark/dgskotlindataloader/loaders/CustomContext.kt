package me.fluffshark.dgskotlindataloader.loaders

import kotlin.reflect.KClass
import java.util.UUID
import java.util.UUID.randomUUID

class CustomContext(private val registry: CoroutineDataLoaderRegistry) {

  val id: UUID = randomUUID()

  fun <K, V, T : CoroutineMappedBatchLoader<K, V>> getDataLoader(c: KClass<T>): CoroutineDataLoader<K, V> =
    registry.getDataLoader(c)

    fun <K, V, T : CoroutineMappedBatchLoaderWithContext<K, V>> getDataLoaderWithContext(
      c: KClass<T>
    ): CoroutineDataLoader<K, V> = registry.getDataLoaderWithContext(c, this)
}