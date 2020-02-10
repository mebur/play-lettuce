package com.github.mebur.playlettuce

import java.util.Optional
import java.util.concurrent.{Callable, CompletionStage}

import javax.inject.Inject
import akka.Done
import play.api.Configuration
import play.cache.{AsyncCacheApi, SyncCacheApi}

import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.compat.java8.OptionConverters._

/** Java-compatible wrapper that implements [[play.cache.AsyncCacheApi]]
  *
  * @param acache A [[LettuceCacheApi]] instance to be wrapped
  * @param configuration The application configuration
  * @param ec The execution context
  */
class JavaAsyncWrapper @Inject()(val acache: LettuceCacheApi, val configuration: Configuration)
                                (implicit val ec: ExecutionContext) extends AsyncCacheApi {

  override def sync(): SyncCacheApi = new JavaSyncWrapper(acache, configuration)(ec)

  override def get[T](key: String): CompletionStage[Optional[T]] = {
    // NOTE: This is a bit weird and non-idiomatic but it's the only way it compiles
    //noinspection GetOrElseNull
    acache.javaGet[T](key).map(_.asJava).toJava
  }

  override def set(key: String, value: scala.Any): CompletionStage[Done] = {
    acache.set(key, value).toJava
  }

  override def set(key: String, value: scala.Any, expiration: Int): CompletionStage[Done] = {
    acache.set(key, value, Duration(expiration, "seconds")).toJava
  }

  override def getOrElseUpdate[T](key: String, block: Callable[CompletionStage[T]]): CompletionStage[T] = {
    acache.javaGetOrElseUpdate[T](key, Duration.Inf) {
      block.call().toScala
    }.toJava
  }

  override def getOrElseUpdate[T](key: String, block: Callable[CompletionStage[T]], expiration: Int): CompletionStage[T] = {
    acache.javaGetOrElseUpdate[T](key, Duration(expiration, "seconds")) {
      block.call().toScala
    }.toJava
  }

  override def remove(key: String): CompletionStage[Done] = {
    acache.remove(key).toJava
  }

  override def removeAll(): CompletionStage[Done] = {
    acache.removeAll().toJava
  }
}
