package me.fluffshark.dgskotlindataloader.loaders

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

internal abstract class AbstractCoroutineDataLoader<K, V>(private val maxBatchSize: Int): CoroutineDataLoader<K, V> {
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
    // Java's Concurrent data structures don't block on reads... so these optimistic reads
    // should be fast and will help minimize locking.
    if (fetchedKeys.contains(key)) {
      return fetchedValues[key]
    }

    // Otherwise add this key to the queue, and kick off a new job. This fetchJob?.join() happens
    // in a loop because the _current_ fetchJob might get canceled by another call to load() before its
    // delay() has elapsed. We need one job to _actually_ complete.
    queuedKeys.add(key)
    restartLoadJob()
    while (!fetchedKeys.contains(key)) {
      fetchJob.get()?.join()
    }
    return fetchedValues[key]
  }

  /**
   * Kick off a job which waits for 1 millisecond and then loads everything from keysToFetch into fetchedValues.
   * If an existing job is queued, cancel it and restart with a new one.
   */
  private suspend fun restartLoadJob() = coroutineScope {
    fetchJobLock.withLock {
      fetchJob.get()?.cancel()
      fetchJob.set(
        launch {
          delay(1)
          // fetchDataLock makes sure we don't load keys twice if `loader.load()` takes longer than `delay()`
          fetchDataLock.withLock {
            val snapshot = queuedKeys.toSet()
            val valueMap = if (snapshot.size >= maxBatchSize) {
              val batches = snapshot.chunked(maxBatchSize)
              val values = batches.pmap { doLoad(it.toSet()) }
              mutableMapOf<K, V>().apply {
                values.forEach(::putAll)
              }
            } else {
              doLoad(snapshot)
            }
            fetchedValues.putAll(valueMap.filterValues { it != null })
            queuedKeys.removeAll(snapshot)
            fetchedKeys.addAll(snapshot)
          }
        }
      )
    }
  }
}