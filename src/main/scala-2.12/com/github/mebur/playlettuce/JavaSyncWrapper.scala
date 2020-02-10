package com.github.mebur.playlettuce

import java.util.Optional
import java.util.concurrent.Callable

import javax.inject.Inject
import play.api.Configuration
import play.cache.SyncCacheApi

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.compat.java8.OptionConverters._

/** A wrapper of the default [[LettuceCacheApi]] that provides [[play.cache.SyncCacheApi]]
  *
  * @param acache A [[LettuceCacheApi]] instance to wrap
  * @param configuration The application configuration
  * @param ec An execution context
  */
class JavaSyncWrapper @Inject()(val acache: LettuceCacheApi, val configuration: Configuration)
                               (implicit val ec: ExecutionContext) extends SyncCacheApi with TimeoutConfigurable {

  override def get[T](key: String): Optional[T] = {
    // NOTE: This is a bit weird and non-idiomatic but it's the only way it compiles
    //noinspection GetOrElseNull
    Await.result(
      acache.javaGet[T](key).map(_.asJava),
      timeout
    )
  }

  override def getOrElseUpdate[T](key: String, block: Callable[T]): T = {
    Await.result(
      acache.javaGetOrElseUpdate[T](key, Duration.Inf) {
        Future {
          block.call()
        }
      },
      timeout
    )
  }

  override def getOrElseUpdate[T](key: String, block: Callable[T], expiration: Int): T = {
    Await.result(
      acache.javaGetOrElseUpdate[T](key, Duration(expiration, "seconds")) {
        Future {
          block.call()
        }
      },
      timeout
    )
  }

  override def set(key: String, value: scala.Any): Unit = {
    Await.result(
      acache.set(key, value),
      timeout
    )
  }

  override def set(key: String, value: scala.Any, expiration: Int): Unit = {
    Await.result(
      acache.set(key, value, Duration(expiration, "seconds")),
      timeout
    )
  }

  override def remove(key: String): Unit = {
    Await.result(
      acache.remove(key),
      timeout
    )
  }

}
