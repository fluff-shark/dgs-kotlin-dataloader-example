package me.fluffshark.dgskotlindataloader

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import graphql.schema.DataFetchingEnvironment
import me.fluffshark.dgskotlindataloader.Show
import java.util.concurrent.CompletableFuture

@DgsComponent
class ShowsDataFetcher {
    @DgsQuery
    fun shows(): List<Show> {
      return showMap.keys.map { Show(it, null) }.toList()
    }

    @DgsData(parentType = "Show")
    fun releaseYear(dfe: DataFetchingEnvironment): CompletableFuture<Int> {
      val show = dfe.getSource<Show>()
      val loader = dfe.getDataLoader<String, Show>("ShowDataLoader")
      return loader.load(show.title).thenApply { it.releaseYear }
    }
}