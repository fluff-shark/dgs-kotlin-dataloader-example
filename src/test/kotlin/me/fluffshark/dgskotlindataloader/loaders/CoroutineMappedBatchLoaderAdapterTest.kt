package me.fluffshark.dgskotlindataloader.loaders

import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineMappedBatchLoaderAdapterTest {
  @Test
  fun `should fetch from the underlying loader on a delay`(): Unit = runTest {
    val loader = TestCoroutineLoader()
    val adapter = CoroutineMappedBatchLoaderAdapter(loader, Integer.MAX_VALUE)
    val startTime = currentTime
    assertEquals(adapter.load(1), "1")
    assertEquals(currentTime, startTime + 1)
    assertEquals(loader.getCallArgs(), listOf(setOf(1)))
  }

  @Test
  fun `should handle null values from DataLoader impls gracefully`(): Unit = runTest {
    val loader = NullCoroutineLoader()
    val adapter = CoroutineMappedBatchLoaderAdapter(loader, Integer.MAX_VALUE)
    assertNull(adapter.load(1))
    assertNull(adapter.load(1))
    assertEquals(loader.getCallArgs(), listOf(setOf(1)))
  }

  @Test
  fun `should handle incomplete maps from DataLoader impls gracefully`(): Unit = runTest {
    val loader = IncompleteCoroutineLoader()
    val adapter = CoroutineMappedBatchLoaderAdapter(loader, Integer.MAX_VALUE)
    assertNull(adapter.load(1))
    assertNull(adapter.load(1))
    assertEquals(loader.getCallArgs(), listOf(setOf(1)))
  }

  @Test
  fun `should batch calls which are made within the debounce delay time`(): Unit = runTest {
    val loader = TestCoroutineLoader()
    val adapter = CoroutineMappedBatchLoaderAdapter(loader, Integer.MAX_VALUE)
    val startTime = currentTime
    val job1 = async { adapter.load(1) }
    val job2 = async { adapter.load(2) }
    val fetched1 = job1.await()
    val fetched2 = job2.await()
    assertEquals(fetched1, "1")
    assertEquals(fetched2, "2")
    assertEquals(currentTime, startTime + 1)
    assertEquals(loader.getCallArgs(), listOf(setOf(1, 2)))
  }

  @Test
  fun `should batch calls made with loadMany`(): Unit = runTest {
    val loader = TestCoroutineLoader()
    val adapter = CoroutineMappedBatchLoaderAdapter(loader, Integer.MAX_VALUE)
    val startTime = currentTime
    val job1 = async { adapter.loadMany(listOf(1, 2)) }
    val job2 = async { adapter.load(3) }
    val fetched1 = job1.await()
    val fetched2 = job2.await()
    assertEquals(fetched1, listOf("1", "2"))
    assertEquals(fetched2, "3")
    assertEquals(currentTime, startTime + 1)
    assertEquals(loader.getCallArgs(), listOf(setOf(1, 2, 3)))
  }

  @Test
  fun `should make parallel calls in separate batches if too many requsts have been queued`(): Unit = runTest {
    val loader = TestCoroutineLoader()
    val adapter = CoroutineMappedBatchLoaderAdapter(loader, 1)
    val startTime = currentTime
    val job1 = async { adapter.load(1) }
    val job2 = async { adapter.load(2) }
    val fetched1 = job1.await()
    val fetched2 = job2.await()
    assertEquals(fetched1, "1")
    assertEquals(fetched2, "2")
    assertEquals(currentTime, startTime + 1)
    assertEquals(loader.getCallArgs(), listOf(setOf(1), setOf(2)))
  }

  @Test
  fun `should handle null values when the calls are batched separately`(): Unit = runTest {
    val loader = NullCoroutineLoader()
    val adapter = CoroutineMappedBatchLoaderAdapter(loader, 1)
    val job1 = async { adapter.load(1) }
    val job2 = async { adapter.load(2) }
    assertNull(job1.await())
    assertNull(job2.await())
  }

  @Test
  fun `should handle incomplete maps from DataLoader impls when the calls are batched separately`(): Unit = runTest {
    val loader = IncompleteCoroutineLoader()
    val adapter = CoroutineMappedBatchLoaderAdapter(loader, 1)
    val job1 = async { adapter.load(1) }
    val job2 = async { adapter.load(2) }
    assertNull(job1.await())
    assertNull(job2.await())
  }

  @Test
  fun `should separate calls which arrive too late`(): Unit = runTest {
    val loader = TestCoroutineLoader()
    val adapter = CoroutineMappedBatchLoaderAdapter(loader, Integer.MAX_VALUE)
    val startTime = currentTime
    adapter.load(1)
    adapter.load(2)
    assertEquals(currentTime, startTime + 2)
    assertEquals(loader.getCallArgs(), listOf(setOf(1), setOf(2)))
  }

  @Test
  fun `should not delay and return a cached value if it can`(): Unit = runTest {
    val loader = TestCoroutineLoader()
    val adapter = CoroutineMappedBatchLoaderAdapter(loader, Integer.MAX_VALUE)
    val startTime = currentTime
    adapter.load(1)
    adapter.load(1)
    assertEquals(currentTime, startTime + 1)
    assertEquals(loader.getCallArgs(), listOf(setOf(1)))
  }

  @Test
  fun `should bubble up errors from the underyling loader`(): Unit = runTest {
    val loader = ErrorThrowingCoroutineLoader()
    val adapter = CoroutineMappedBatchLoaderAdapter(loader, Integer.MAX_VALUE)
    assertThrows<IntentionalTestException> { adapter.load(1) }
  }
}

/** Loader which always returns null. */
private class NullCoroutineLoader : CallCountingCoroutineLoader<Int, String?>() {
    override suspend fun privateLoad(keys: Set<Int>): Map<Int, String?> {
      return keys.map { it to null }.toMap()
    }
}

/** Loader which doesn't include all the keys from the input set into its values. */
private class IncompleteCoroutineLoader : CallCountingCoroutineLoader<Int, String?>() {
  override suspend fun privateLoad(keys: Set<Int>): Map<Int, String> {
    return mapOf()
  }
}

/** Loader which throws an error. */
private class ErrorThrowingCoroutineLoader : CallCountingCoroutineLoader<Int, String?>() {
  override suspend fun privateLoad(keys: Set<Int>): Map<Int, String> {
    throw IntentionalTestException()
  }
}

private class IntentionalTestException : RuntimeException("Exception used for testing.")