# Summary

This is a sample [DGS](https://netflix.github.io/dgs/) project which shows how to implement DataLoaders
using kotlin's `suspend`.

Major thanks to @lennyburdette for the inspiration of using debounce in [this gist](https://gist.github.com/lennyburdette/f3fe6ae7a498698774cc95d1bfc956b4).

## Key files

- [CoroutineDataLoader](./src/main/kotlin/me/fluffshark/dgskotlindataloader/loaders/CoroutineDataLoader.kt): DataFetcher-facing API
- [CoroutineMappedBatchLoader](./src/main/kotlin/me/fluffshark/dgskotlindataloader/loaders/CoroutineMappedBatchLoader.kt) and [CoroutineMappedBatchLoaderWithContext](./src/main/kotlin/me/fluffshark/dgskotlindataloader/loaders/CoroutineMappedBatchLoaderWithContext.kt): Loader interfaces to implement
- [ShowDataLoader](./src/main/kotlin/me/fluffshark/dgskotlindataloader/ShowDataLoader.kt): Sample implementation
- [DataFetcher](./src/main/kotlin/me/fluffshark/dgskotlindataloader/DataFetcher.kt): Sample caller

## Demo

Run the server, visit http://localhost:8080/graphiql in a browser, and run the query:

```
{
    shows {
        title
        releaseYear
    }
}
```

Note the terminal output, which should look something like this:

```
Calling load(Stranger Things)
Calling load(Ozark)
Calling load(The Crown)
Calling load(Dead to Me)
Calling load(Orange is the New Black)
Running load([Dead to Me, Stranger Things, Orange is the New Black, The Crown, Ozark])
```

Those come from [here](https://github.com/fluff-shark/dgs-kotlin-dataloader-example/blob/cc353a004887528908248ea3932a5956933fd3ae/src/main/kotlin/me/fluffshark/dgskotlindataloader/DataFetcher.kt#L25) and [here](https://github.com/fluff-shark/dgs-kotlin-dataloader-example/blob/cc353a004887528908248ea3932a5956933fd3ae/src/main/kotlin/me/fluffshark/dgskotlindataloader/ShowDataLoader.kt#L13), showing how the loads are batched.

## Why do this?

### Unify the concurrency models

DGS uses `java-dataloader`, which is built on Java's [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html).
Kotlin's concurrency model uses [coroutines](https://kotlinlang.org/docs/coroutines-guide.html) via the `suspend` keyword.
Switching between the two requires calls to `.await()` and `asCompletableFuture()` from [kotlinx-coroutines-core](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.future/), which is a bit annoying.

Related issues:

- https://github.com/Netflix/dgs-framework/discussions/539
- https://github.com/Netflix/dgs-framework/issues/1074

### Support multiple dataloaders when resolving the same field

`graphql-java` doesn't support the use of multiple DataLoaders to resolve the same field (https://github.com/graphql-java/graphql-java/issues/1198).
DGS shares this limitation, since it uses `graphql-java` under the hood.

This limitation forces implementers to call `.dispatch()` manually inside their DataFetchers to prevent
requests from hanging... but of course that negates the value of using a DataLoader at all.

This approach circumvents that issue.

## Why _not_ do this?

There is a [hardcoded debounce period](https://github.com/fluff-shark/dgs-kotlin-dataloader-example/blob/05a76c12e7e0f6a13596bf4fd31916963b459a1c/src/main/kotlin/me/fluffshark/dgskotlindataloader/loaders/AbstractCoroutineDataLoader.kt#L61)
separating [the calls to `loader.load()`](https://github.com/fluff-shark/dgs-kotlin-dataloader-example/blob/05a76c12e7e0f6a13596bf4fd31916963b459a1c/src/main/kotlin/me/fluffshark/dgskotlindataloader/DataFetcher.kt#L26)
and the start of [the DataLoader implementation](https://github.com/fluff-shark/dgs-kotlin-dataloader-example/blob/05a76c12e7e0f6a13596bf4fd31916963b459a1c/src/main/kotlin/me/fluffshark/dgskotlindataloader/ShowDataLoader.kt#L12).

This is a compromise with no perfect option. The longer the `delay`, the more latency added to each query. Too short of a `delay` and
the calls to the DataLoader impl won't get batched as effectively as they could be.

Depending on your performance needs and scale, this may or may not be a problem for you.