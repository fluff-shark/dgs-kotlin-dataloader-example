package me.fluffshark.dgskotlindataloader.loaders

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class AbstractCoroutineDataLoader<K, V>(private val maxBatchSize: Int) : CoroutineDataLoader<K, V> {
  // Set of keys which callers tried to load() that have _not_ been fetched yet
  private val queuedKeys = Collections.newSetFromMap(ConcurrentHashMap<K, Boolean>())
  // Set of keys whose values exist in fetchedValues. This is a superset of fetchedValues.keys() because
  // data loaders can return 'null' and the ConcurrentHashMap doesn't allow null values.
  //
  // This should only be updated _after_ the values exist in fetchedValues.
  private val fetchedKeys = Collections.newSetFromMap(ConcurrentHashMap<K, Boolean>())
  // Values for any keys which have been fetched.
  private val fetchedValues = ConcurrentHashMap<K, V>()
  // fetchDataLock makes sure we're only fetching one set of data at a time.
  private val fetchDataLock = Mutex()

  // fetchJob is a task which loads queuedKeys and populates fetchedKeys and fetchedValues
  private val fetchJob: AtomicReference<Job?> = AtomicReference(null)
  // fetchJobLock makes sure fetchJob gets canceled & relaunched atomically.
  private val fetchJobLock = Mutex()

  /** Load one batch of data from the underlying loader. */
  abstract suspend fun doLoad(keys: Set<K>): Map<K, V>

  override suspend fun load(key: K): V? {
    // Java's Concurrent data structures do not block on reads... so these optimistic reads should be
    // fast and will help minimize locking.
    if (fetchedKeys.contains(key)) {
      return fetchedValues[key]
    }

    // Add the key to the queue of keys to be fetched.
    queuedKeys.add(key)
    return fetchAndReturnValue(key)
  }

  /**
   * We only allow one fetch job to be running at a time. A fetch job loads values based on keys
   * from queuedKeys into `fetchedValues` and `fetchedKeys`.
   *
   * First, check if value already exists in `fetchedValues`. If it does, return it.
   *
   * If not, check if there is an existing job and wait until it finishes. After it finishes, check
   * if the key is already fetched again. If it is, return its value.
   *
   * If not, start a new job to fetch the key. This time, we should wait for the job to finish
   * before we check if the key is already fetched. If not, it means the key we look for doesn't
   * exist.
   */
  private suspend fun fetchAndReturnValue(key: K): V? = coroutineScope {
    // We use a job lock here to make sure we don't start two fetch jobs at the same time. Also, this lock suspends
    // the coroutine until the fetch job is done. So we automatically wait for the fetch job to finish before
    // we check if the key is already fetched.
    fetchJobLock.withLock {
      // If the key is already fetched, return its value.
      if (fetchedKeys.contains(key)) {
        return@coroutineScope fetchedValues[key]
      }

      // If the key is not fetched, it means the key we look for wasn't included in `queuedKeys` when the job
      // started. Thus, we should restart the job with the key we're looking for.
      if (fetchJob.get() != null) {
        // If a job already exists, wait for it to finish before kicking off a new one.
        // Normally, the `fetchJobLock` is released when a job is done. However exceptions or cancellations
        // can cause lock to be released before the job is done. So we need to wait for the job to finish.
        fetchJob.get()?.join()
      }

      // There is a chance that last finished job already fetched the key we're looking for. If so, return its value.
      if (fetchedKeys.contains(key)) {
        return@coroutineScope fetchedValues[key]
      }

      // Start a job that fetches all keys in `queuedKeys` and populates `fetchedKeys` and `fetchedValues`.
      val newJob = launch {
        // 1-millisecond grace period to allow for more keys to be added to `queuedKeys`.
        delay(1)
        // fetchDataLock makes sure we don't load keys twice if `loader.load()` takes longer than `delay()`
        fetchDataLock.withLock {
          // Take a snapshot of the keys that we are fetching. This is important because we want to remove
          // the keys from `queuedKeys` only after we have fetched the values for them.
          val snapshot = queuedKeys.toSet()
          try {
            val valueMap =
                if (snapshot.size >= maxBatchSize) {
                  val batches = snapshot.chunked(maxBatchSize)
                  val values = batches.pmap { doLoad(it.toSet()) }
                  mutableMapOf<K, V>().apply { values.forEach(::putAll) }
                } else {
                  doLoad(snapshot)
                }
            fetchedValues.putAll(valueMap.filterValues { it != null })
            fetchedKeys.addAll(snapshot)
          } finally {
            // If we fail to fetch the values, we should remove the keys from `queuedKeys`.
            // Otherwise, the keys will be stuck in `queuedKeys` forever.
            queuedKeys.removeAll(snapshot)
          }
        }
      }
      // Update the current running fetch job.
      fetchJob.set(newJob)

      // Wait until the job is done
      newJob.join()

      // At this point, our key should be in `fetchedKeys` and we should have its value in `fetchedValues`.
      // If not, it means that there was an error fetching the value so we'll just return null.
      return@coroutineScope fetchedValues[key]
    }
  }
}
