package me.fluffshark.dgskotlindataloader.loaders

/**
 * Like the [DgsDataLoader] annotation, but used to register [CoroutineMappedBatchLoader]s
 * and [CoroutineMappedBatchLoaderWithContext].
 *
 * We can't use DgsDataLoader because DGS throws an InvalidDataLoaderTypeException on startupif you use
 * that to gat something which doesn't implement BatchLoader from java-dataloader.
 */
@Target(AnnotationTarget.CLASS)
annotation class KotlinDataLoader(
  /** To be honest I'm not sure why we'd need this. Can probably be removed. */
  val name: String,
  /**
   * The max number of elements callers are allowed to send in the key sent to
   * this loader's laod() method in a single call.
   */
  val maxBatchSize: Int = Integer.MAX_VALUE
)