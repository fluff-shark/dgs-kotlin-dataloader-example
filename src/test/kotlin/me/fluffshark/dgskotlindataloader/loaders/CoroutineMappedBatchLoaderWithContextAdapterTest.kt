package me.fluffshark.dgskotlindataloader.loaders

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired

/**
 * Most tests are in [CoroutineMappedBatchLoaderAdapterTest], since almost all the useful
 * code for these classes is in a shared base class. This just confirms the context-specific functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest(
  classes = [
    CoroutineDataLoaderProvider::class
  ]
)
class CoroutineMappedBatchLoaderWithContextAdapterTest(
  @Autowired private val provider: CoroutineDataLoaderProvider
) {
  @Test
  fun `should forward the context into loads`(): Unit = runTest {
    val context = CustomContext(CoroutineDataLoaderRegistry(provider))
    val loader = TestCoroutineLoaderWithContext()
    val adapter = CoroutineMappedBatchLoaderWithContextAdapter(loader, context, Integer.MAX_VALUE)
    assertEquals(adapter.load(1), "${context.id} 1")
  }
}