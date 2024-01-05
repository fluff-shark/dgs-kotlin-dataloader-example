package me.fluffshark.dgskotlindataloader

import com.netflix.graphql.dgs.DgsDataLoader
import org.dataloader.MappedBatchLoader
import java.util.concurrent.CompletionStage
import java.util.concurrent.CompletableFuture

@DgsDataLoader(name = "ShowDataLoader")
class ShowDataLoader : MappedBatchLoader<String, Show> {
    override fun load(keys: MutableSet<String>): CompletionStage<MutableMap<String, Show>> {
      val shows = keys.map { Show(it, showMap.get(it)) }
      val showsByName = shows.associateBy { it.title }.toMutableMap()
      return CompletableFuture.completedFuture(showsByName)
    }
}