package me.fluffshark.dgskotlindataloader.loaders

import me.fluffshark.dgskotlindataloader.loaders.CoroutineDataLoaderProvider.Companion.IllegalKotlinDataLoaderMetadataException
import me.fluffshark.dgskotlindataloader.loaders.CoroutineDataLoaderProvider.Companion.IllegalKotlinDataLoaderTypeException
import me.fluffshark.dgskotlindataloader.loaders.CoroutineDataLoaderProvider.Companion.IllegalScopeException
import me.fluffshark.dgskotlindataloader.loaders.CoroutineDataLoaderProvider.Companion.UnknownDataLoaderException
import org.springframework.beans.factory.annotation.Autowired

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@SpringBootTest(
  classes = [
    CoroutineDataLoaderProvider::class,
    TestCoroutineLoader::class,
    TestCoroutineLoaderWithContext::class,
  ]
)
class CoroutineDataLoaderProviderTest(
  @Autowired private val provider: CoroutineDataLoaderProvider
) {
  @Test
  fun `should parse metadata and create instances of CoroutineMappedBatchLoader`() {
    val metadata = provider.getInstanceAndMetadata(TestCoroutineLoader::class)
    assertEquals(TEST_COROUTINE_LOADER_NAME, metadata.name)
    assertEquals(TEST_COROUTINE_LOADER_BATCH_SIZE, metadata.maxBatchSize)
    assertInstanceOf(TestCoroutineLoader::class.java, metadata.instance)
  }

  @Test
  fun `should parse metadata and create instances of CoroutineMappedBatchLoaderWithContext`() {
    val metadata = provider.getInstanceAndMetadata(TestCoroutineLoaderWithContext::class)
    assertEquals(TEST_COROUTINE_LOADER_WITH_CONTEXT_NAME, metadata.name)
    assertEquals(TEST_COROUTINE_LOADER_WITH_CONTEXT_BATCH_SIZE, metadata.maxBatchSize)
    assertInstanceOf(TestCoroutineLoaderWithContext::class.java, metadata.instance)
  }

  @Test
  fun `should throw the right exception if asked for an unknown class`() {
    assertThrows<UnknownDataLoaderException> {
      provider.getInstanceAndMetadata(UntaggedDataLoader()::class)
    }
  }
}

private class UntaggedDataLoader : CoroutineMappedBatchLoader<Int, String> {
    override suspend fun load(keys: Set<Int>): Map<Int, String> { return mapOf() }
}

@SpringBootTest(classes = [ConfigurableApplicationContext::class, ImproperlyTaggedClassTest.Config::class])
class ImproperlyTaggedClassTest(@Autowired private val context: ConfigurableApplicationContext) {
  @Configuration
  class Config {
    @KotlinDataLoader(name = "badClass")
    private class ClassWithoutValidInterface
  }
  @Test
  fun `should throw the right error when the KotlinDataLoader annotation is applied to an invalid type`() {
    assertThrows<IllegalKotlinDataLoaderTypeException> { CoroutineDataLoaderProvider(context) }
  }
}

@SpringBootTest(classes = [ConfigurableApplicationContext::class, ImproperlyScopedClassTest.Config::class])
class ImproperlyScopedClassTest(@Autowired private val context: ConfigurableApplicationContext) {
  @Configuration
  class Config {
    @KotlinDataLoader(name = "classWithInvalidScope")
    @Scope("prototype")
    private class ClassWithInvalidScope : CoroutineMappedBatchLoader<Int, String> {
      override suspend fun load(keys: Set<Int>): Map<Int, String> { return mapOf() }
    }
  }

  @Test
  fun `should throw an IllegalScopeException when the data loader is not a singleton`() {
    assertThrows<IllegalScopeException> { CoroutineDataLoaderProvider(context) }
  }
}

@SpringBootTest(classes = [ConfigurableApplicationContext::class, InvalidBatchSizeTest.Config::class])
class InvalidBatchSizeTest(@Autowired private val context: ConfigurableApplicationContext) {
  @Configuration
  class Config {
    @KotlinDataLoader(name = "classWithInvalidBatchSize", maxBatchSize = -1)
    @Scope("prototype")
    private class ClassWithInvalidBatchSize : CoroutineMappedBatchLoader<Int, String> {
      override suspend fun load(keys: Set<Int>): Map<Int, String> { return mapOf() }
    }
  }

  @Test
  fun `should throw an IllegalKotlinDataLoaderMetadataException if the max batch size is negative`() {
    assertThrows<IllegalKotlinDataLoaderMetadataException> { CoroutineDataLoaderProvider(context) }
  }
}
