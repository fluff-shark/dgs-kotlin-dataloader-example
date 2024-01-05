package me.fluffshark.dgskotlindataloader.loaders

import me.fluffshark.dgskotlindataloader.loaders.KotlinDataLoader

/**
 * A poor man's spyk (https://mockk/io/#spy) because of https://github.com/mockk/mockk/issues/554.
 */
internal abstract class CallCountingCoroutineLoader<K, V> : CoroutineMappedBatchLoader<K, V> {
  private val callArgs = mutableListOf<Set<K>>()

  override suspend fun load(keys: Set<K>): Map<K, V> {
    callArgs.add(keys)
    return privateLoad(keys)
  }

  fun getCallArgs(): List<Set<K>> {
    return callArgs
  }

  protected abstract suspend fun privateLoad(keys: Set<K>): Map<K, V>
}

internal const val TEST_COROUTINE_LOADER_NAME = "testCoroutineLoader"
internal const val TEST_COROUTINE_LOADER_BATCH_SIZE = 50

@KotlinDataLoader(name = TEST_COROUTINE_LOADER_NAME, maxBatchSize = TEST_COROUTINE_LOADER_BATCH_SIZE)
internal class TestCoroutineLoader : CallCountingCoroutineLoader<Int, String>() {
  override suspend fun privateLoad(keys: Set<Int>): Map<Int, String> {
    return keys.associateWith { it.toString() }
  }
}

internal const val TEST_COROUTINE_LOADER_WITH_CONTEXT_NAME = "testCoroutineLoaderWithContext"
internal const val TEST_COROUTINE_LOADER_WITH_CONTEXT_BATCH_SIZE = 20

@KotlinDataLoader(
  name = TEST_COROUTINE_LOADER_WITH_CONTEXT_NAME,
  maxBatchSize = TEST_COROUTINE_LOADER_WITH_CONTEXT_BATCH_SIZE
)
internal class TestCoroutineLoaderWithContext : CoroutineMappedBatchLoaderWithContext<Int, String> {
  override suspend fun load(keys: Set<Int>, context: CustomContext): Map<Int, String> {
    return keys.associateWith { "${context.id} ${it}" }
  }
}