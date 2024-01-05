package me.fluffshark.dgskotlindataloader.loaders

import io.mockk.mockk
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertSame

@SpringBootTest(
  classes = [
    CoroutineDataLoaderProvider::class,
    ConfigurableApplicationContext::class,
    TestCoroutineLoader::class,
    TestCoroutineLoaderWithContext::class,
  ]
)
class CoroutineDataLoaderRegistryTest(
  @Autowired private val provider: CoroutineDataLoaderProvider
) {
  @Test
  fun `should return the same instance when passed the same class twice`() {
    val registry = CoroutineDataLoaderRegistry(provider)
    assertSame(
      registry.getDataLoaderWithContext(TestCoroutineLoaderWithContext::class, mockk<CustomContext>()),
      registry.getDataLoaderWithContext(TestCoroutineLoaderWithContext::class, mockk<CustomContext>()),
    )
  }
}