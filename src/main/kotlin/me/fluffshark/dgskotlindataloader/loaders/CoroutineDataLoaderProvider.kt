package me.fluffshark.dgskotlindataloader.loaders

import org.springframework.context.ConfigurableApplicationContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

/**
 * The goals here are to:
 *
 *   1. Find all classes tagged by @KotlinDataLoder
 *   2. Make sure they're bound to singleton scope and implement exactly one of the supported data loader interfaces.
 *   3. Create those singleton instances and parse the metadata from their annotations.
 *
 * Doing this work eagrly on startup will:
 *
 *   1. Make sure the server fails fast in case of code bugs (wrong scopes, interaces, etc) w/ decent error messages.
 *   2. Minimize the work done during requests, to minimize latency.
 */
@Component
class CoroutineDataLoaderProvider(
  @Autowired private val applicationContext: ConfigurableApplicationContext
) {

  private val metadata: ConcurrentMap<KClass<*>, DataLoaderInfo<*>> = ConcurrentHashMap()
  init {
    applicationContext.getBeansWithAnnotation(KotlinDataLoader::class.java)
      .forEach { (name, bean) ->
        val beanClass = bean::class
        val annotation = beanClass.findAnnotation<KotlinDataLoader>()
        if (annotation != null) {
          metadata[beanClass] = DataLoaderInfo(
            instance = bean,
            name = annotation.name,
            maxBatchSize = assertValidBatchSize(beanClass, annotation.maxBatchSize),
          )
        }
        assertExtendsExpected(beanClass)
        assertSingleton(beanClass, applicationContext.beanFactory.getMergedBeanDefinition(name).scope)
      }
  }

  @Throws(UnknownDataLoaderException::class)
  fun <T: Any> getInstanceAndMetadata(loaderClass: KClass<T>): DataLoaderInfo<T> {
    // This cast is safe because "metadata" is only mutated by init, and init only adds key/value
    // pairs when the class key matches the DataLoaderInfo instance.
    @Suppress("UNCHECKED_CAST")
    return metadata[loaderClass] as DataLoaderInfo<T>? ?: throw UnknownDataLoaderException(loaderClass)
  }

  companion object {
    private val allowedTypes =
      listOf(CoroutineMappedBatchLoader::class, CoroutineMappedBatchLoaderWithContext::class)

    @Throws(IllegalKotlinDataLoaderTypeException::class)
    private fun assertExtendsExpected(c: KClass<*>) {
      if (allowedTypes.filter { c.isSubclassOf(it) }.size != 1) {
        throw IllegalKotlinDataLoaderTypeException(c, allowedTypes)
      }
    }

    /**
     * Throw a NullPointerException if scope is null, and an IllegalScopeException if it's not "Singleton".file
     *
     * The Spring docs are a bit vague here. Says value will be null "if not known yet."
     * I haven't seen this happen in practice, so this is more of a safeguard in case the impl
     * changes in future versions of Spring.
     */
    private fun assertSingleton(c: KClass<*>, scope: String?) {
      if (scope == null) {
        val msg = "Scope of bean ${c.qualifiedName} is null. This bootstrap code won't work as intended."
        throw NullPointerException(msg)
      }
      if (scope != "singleton") {
        throw IllegalScopeException(c, scope)
      }
    }

    private fun assertValidBatchSize(c: KClass<*>, batchSize: Int): Int {
      if (batchSize <= 0) {
        throw IllegalKotlinDataLoaderMetadataException(
          "Invalid @KotlinDataLoader definition on ${c.qualifiedName}. " +
            "maxBatchSize must be greater than 0, but found $batchSize."
        )
      }
      return batchSize
    }

    /**
     * A singleton instance of a class annotated by [KotlinDataLoader], alongside the metadata
     * in its annotation.
     */
    data class DataLoaderInfo<T: Any>(
      /** The DataLoader instance. */
      val instance: T,
      /** The [KotlinDataLoader] name. */
      val name: String,
      /** The [KotlinDataLoader] maxBatchSize. */
      val maxBatchSize: Int
    )

    /**
     * Exception thrown when someone binds a data loader to something _besides_ singleton scope.file
     *
     * I don't believe there's any good reason to do this... and we can optimize performance by
     * prohibiting it and eagerly instantiating the instance of each loader on app startup.
     */
    class IllegalScopeException(loader: KClass<*>, scope: String) : RuntimeException(
      "CoroutineDataLoader ${loader.qualifiedName} must have \"singleton\" scope. " +
        "Found: $scope. Please remove any annotations which might change the Spring scope."
    )

    /**
     * Exception thrown when someone sticks a @KotlinDataLoader annotation on a type which _doesn't_ implement
     * one of the supported data loader interfaces.
     */
    class IllegalKotlinDataLoaderTypeException(loader: KClass<*>, allowedTypes: List<KClass<*>>): RuntimeException(
      "Class ${loader.qualifiedName} is annotated by @KotlinDataLoader, but does not implement one " +
        "of the expected interfaces. It must extend one (and only one!) of ${allowedTypes.map { it.qualifiedName }}"
    )

    /**
     * Exception thrown when a DataFetcher asks for a data loader class which was never loaded.
     */
    class UnknownDataLoaderException(c: KClass<*>): RuntimeException(
      "Unknown data loader class: ${c.qualifiedName}. Did you remember to annotate " +
        "it with @KotlinDataLoader?"
    )

    /**
     * Exception thrown if the metadata set in the [KotlinDataLoader] annotation is invalid.
     */
    class IllegalKotlinDataLoaderMetadataException(msg: String): RuntimeException(msg)
  }
}