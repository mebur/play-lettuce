package com.github.mebur.playlettuce

import javax.inject.{Inject, Singleton}
import akka.Done

import scala.reflect.ClassTag
import scala.compat.java8.FutureConverters._
import akka.actor.ActorSystem
import io.lettuce.core.{KeyValue, RedisClient, SetArgs}
import io.lettuce.core.api.async.RedisAsyncCommands
import play.api.Configuration

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try


/** The base implementation of [[LettuceCacheApi]].
  *
  * @param codec A lettuce RedisCodec that can be used to serialize AnyRef
  * @param configuration The application configuration
  * @param name The cache name (or "default" if missing)
  * @param ec The execution context to use
  */
@Singleton
class LettuceClient @Inject() (protected val codec: AkkaCodec, val configuration: Configuration, val name: String = "default")
                              (implicit val ec: ExecutionContext) extends LettuceCacheApi {

  /** The [[io.lettuce.core.RedisClient]] instance that represents the connection to Redis **/
  private val client: RedisClient = RedisClient.create(configuration.get[String](s"lettuce.$name.url"))
  /** The redis commands bound to the specific client and encoder **/
  private val commands: RedisAsyncCommands[String, AnyRef] = client.connect(codec).async()

  /** Do a set on Redis: these are two different method calls depending
    * on what the duration is (infinite is a special case)
    *
    * @param key The key to set
    * @param value The value to set for the key
    * @param expiration The TTL of the entry
    * @return The Redis command result
    */
  private def doSet(key: String, value: Any, expiration: Duration): Future[Any] = {
    expiration match {
      case Duration.Inf =>
        commands.set(
          key,
          value.asInstanceOf[AnyRef]
        ).toScala
      case _ =>
        commands.set(
          key,
          value.asInstanceOf[AnyRef],
          SetArgs.Builder.ex(expiration.toSeconds)
        ).toScala
    }
  }

  override def javaGet[T](key: String): Future[Option[T]] = {
    commands.get(key).toScala map {
      case data: AnyRef =>
        Some(data.asInstanceOf[T])
      case null => None
    }
  }

  override def get[T](key: String)(implicit ctag: ClassTag[T]): Future[Option[T]] = {
    javaGet[T](key)
  }

  override def getAll[T <: AnyRef](keys: Seq[String]): Future[Seq[Option[T]]] = {
    commands.mget(keys: _*).toScala.map(_.asScala.map {
      case data: KeyValue[String, AnyRef] if data.hasValue => Some(data.getValue).asInstanceOf[Option[T]]
      case data: KeyValue[String, AnyRef] if !data.hasValue => None
      case null => None
    }.toSeq)
  }

  override def getOrElseUpdate[A](key: String, expiration: Duration)(orElse: => Future[A])(implicit ctag: ClassTag[A]): Future[A] = {
    javaGetOrElseUpdate[A](key, expiration)(orElse)
  }

  override def javaGetOrElseUpdate[A](key: String, expiration: Duration)(orElse: => Future[A]): Future[A] = {
    commands.get(key).toScala.flatMap({
      case data: AnyRef => Future(data.asInstanceOf[A])
      case null =>
        val orElseFut = orElse
        orElseFut.onComplete(
          (t: Try[A]) => {
            if(t.isSuccess) {
              doSet(key, t.get, expiration)
            }
          }
        )
        orElseFut
    })
  }

  override def set(key: String, value: Any, expiration: Duration): Future[Done] = {
    doSet(key, value, expiration).map(_ => Done)
  }

  override def setAll(keyValues: Map[String, AnyRef]): Future[Done] = {
    commands.mset(
      keyValues.asJava
    ).toScala.map(_ => Done)
  }

  override def remove(key: String): Future[Done] = {
    commands.del(key).toScala.map(_ => Done)
  }

  override def remove(keys: Seq[String]): Future[Done] = {
    commands.del(keys: _*).toScala.map(_ => Done)
  }

  override def removeAll(): Future[Done] = {
    commands.flushdb().toScala.map(_ => Done)
  }
}
