package me.fluffshark.dgskotlindataloader.loaders

import me.fluffshark.dgskotlindataloader.loaders.CustomContext
import com.netflix.graphql.dgs.context.DgsCustomContextBuilder
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component
class CustomContextBuilder(
  @Autowired private val provider: CoroutineDataLoaderProvider,
): DgsCustomContextBuilder<CustomContext> {

    override fun build(): CustomContext {
      val registry = CoroutineDataLoaderRegistry(provider)
      return CustomContext(registry)
    }

}