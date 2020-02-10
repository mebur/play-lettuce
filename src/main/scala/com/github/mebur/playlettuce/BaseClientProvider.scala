package com.github.mebur.playlettuce

import javax.inject.{Inject, Provider}

import akka.actor.ActorSystem
import play.api.Configuration
import play.api.inject.Injector

import scala.concurrent.ExecutionContext


/** Base class for API providers (for dependency injection).
  *
  * Abstracts out useful methods to retrieve the basic API
  * (all Play APIs are implemented as "wrappers" of [[LettuceCacheApi]])
  *
  * @tparam T The interface being provided
  */
abstract class BaseClientProvider[T] extends Provider[T] {
  val configuration: Configuration

  @Inject protected var injector: Injector = _
  @Inject protected var actorSystem: ActorSystem = _
  @Inject protected var codec: AkkaCodec = _

  /** The execution context that the cache uses to execute futures **/
  protected def ec: ExecutionContext = configuration.getOptional[String]("play.cache.dispatcher").map(actorSystem.dispatchers.lookup(_)).getOrElse(injector.instanceOf[ExecutionContext])

  /** Obtains a [[LettuceCacheApi]] instance
    *
    * @param name The cache name (configurations are all per-cache)
    * @return An instance of [[LettuceCacheApi]]
    */
  protected def getLettuceApi(name: String): LettuceCacheApi = {
    new LettuceClient(codec, configuration, name)(ec)
  }
}
