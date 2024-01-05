package me.fluffshark.dgskotlindataloader

import com.netflix.graphql.dgs.DgsDataLoader
import me.fluffshark.dgskotlindataloader.loaders.CoroutineMappedBatchLoader
import me.fluffshark.dgskotlindataloader.loaders.KotlinDataLoader
import java.util.concurrent.CompletionStage
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.delay

@KotlinDataLoader(name = "ShowDataLoader")
class ShowDataLoader : CoroutineMappedBatchLoader<String, Show> {
    override suspend fun load(keys: Set<String>): Map<String, Show> {
      val shows = keys.map { Show(it, showMap.get(it)) }
      val showsByName = shows.associateBy { it.title }.toMutableMap()
      delay(1000) // To help show proper batching w/ a manual query
      return showsByName
    }
}