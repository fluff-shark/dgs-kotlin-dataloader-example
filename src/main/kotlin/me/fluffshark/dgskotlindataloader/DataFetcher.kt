package me.fluffshark.dgskotlindataloader

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.context.DgsContext
import graphql.schema.DataFetchingEnvironment
import me.fluffshark.dgskotlindataloader.Show
import me.fluffshark.dgskotlindataloader.loaders.CustomContext
import java.util.concurrent.CompletableFuture

@DgsComponent
class ShowsDataFetcher {
    @DgsQuery
    fun shows(): List<Show> {
      return showMap.keys.map { Show(it, null) }.toList()
    }

    @DgsData(parentType = "Show")
    suspend fun releaseYear(dfe: DgsDataFetchingEnvironment): Int? {
      val showWithTitleOnly = dfe.getSource<Show>()
      val loader = DgsContext.getCustomContext<CustomContext>(dfe)
        .getDataLoader(ShowDataLoader::class)
      val loaded = loader.load(showWithTitleOnly.title)
      return loaded?.releaseYear
    }
}