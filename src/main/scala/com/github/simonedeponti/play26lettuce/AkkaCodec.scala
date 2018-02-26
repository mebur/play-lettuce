package com.github.simonedeponti.play26lettuce

import java.nio.ByteBuffer
import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import io.lettuce.core.codec.RedisCodec

import scala.collection.mutable


class AkkaCodec(val system: ActorSystem) extends RedisCodec[String, AnyRef] {

  private val serialization = SerializationExtension(system)
  private val charset = Charset.forName("UTF-8")

  override def decodeKey(bytes: ByteBuffer): String = {
    charset.decode(bytes).toString
  }

  override def decodeValue(bytes: ByteBuffer): AnyRef = {
    val serializerId = bytes.getInt
    val serializer = serialization.serializerByIdentity(serializerId)
    val byteArr = new Array[Byte](bytes.remaining())
    bytes.get(byteArr)
    serializer.fromBinary(byteArr)
  }

  override def encodeKey(key: String): ByteBuffer = {
    charset.encode(key)
  }

  override def encodeValue(value: AnyRef): ByteBuffer = {
    val serializer = serialization.findSerializerFor(value)
    val header = ByteBuffer.allocate(4).putInt(serializer.identifier).array()
    ByteBuffer.wrap(header ++ serializer.toBinary(value))
  }
}